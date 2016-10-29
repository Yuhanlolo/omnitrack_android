package kr.ac.snu.hcil.omnitrack.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.utils.isInDozeMode


/*
*
* this will need to be refactored to be a normal service to handle the long retrieval operation of external measure factory values.
 */
class OTBackgroundLoggingService : IntentService("OTBackgroundLoggingService") {

    enum class LoggingSource {
        Trigger, Shortcut
    }

    companion object {
        const val TAG = "BGLoggingService"

        private val ACTION_LOG = "kr.ac.snu.hcil.omnitrack.services.action.LOG"


        private const val INTENT_EXTRA_LOGGING_SOURCE = "loggingSource"

        private val flagPreferences: SharedPreferences by lazy {
            OTApplication.app.getSharedPreferences("pref_background_logging_service", Context.MODE_PRIVATE)
        }

        private fun getLoggingFlag(tracker: OTTracker): Long? {
            return if (flagPreferences.contains(tracker.objectId)) {
                flagPreferences.getLong(tracker.objectId, Long.MIN_VALUE)
            } else null
        }

        /**
         * return: String: tracker id, Long: Timestamp
         */
        fun getFlags(): List<Pair<String, Long>> {
            return flagPreferences.all.entries.map { Pair(it.key, it.value as Long) }
        }

        private fun setLoggingFlag(tracker: OTTracker, timestamp: Long) {
            flagPreferences.edit().putLong(tracker.objectId, timestamp).apply()
        }

        private fun removeLoggingFlag(tracker: OTTracker) {
            flagPreferences.edit().remove(tracker.objectId).apply()
        }

        fun startLoggingInService(context: Context, tracker: OTTracker, source: LoggingSource) {

            context.startService(makeIntent(context, tracker, source))
        }

        fun startLoggingAsync(context: Context, tracker: OTTracker, source: LoggingSource, finished: ((success: Boolean) -> Unit)? = null) {
            val builder = OTItemBuilder(tracker, OTItemBuilder.MODE_BACKGROUND)

            OTApplication.logger.writeSystemLog("start background logging of ${tracker.name}", TAG)
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                OTApplication.logger.writeSystemLog("idleMode: ${isInDozeMode()}", TAG)
            }

            setLoggingFlag(tracker, System.currentTimeMillis())
            sendBroadcast(context, OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_STARTED, tracker)
            builder.autoCompleteAsync {
                val item = builder.makeItem()
                OTApplication.app.dbHelper.save(item, tracker)
                if (item.dbId != null) {
                    sendBroadcast(context, OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_SUCCEEDED, tracker, item.dbId!!)

                    OTApplication.logger.writeSystemLog("${tracker.name} background logging was successful", TAG)
                    removeLoggingFlag(tracker)
                    finished?.invoke(true)
                } else {

                    OTApplication.logger.writeSystemLog("${tracker.name} background logging failed", TAG)
                    removeLoggingFlag(tracker)
                    finished?.invoke(false)
                }
            }
        }

        private fun sendBroadcast(context: Context, action: String, tracker: OTTracker) {
            val intent = Intent(action)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            context.sendBroadcast(intent)
        }

        private fun sendBroadcast(context: Context, action: String, tracker: OTTracker, itemDbId: Long) {
            val intent = Intent(action)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            intent.putExtra(OTApplication.INTENT_EXTRA_DB_ID_ITEM, itemDbId)
            context.sendBroadcast(intent)
        }

        fun makeIntent(context: Context, tracker: OTTracker, source: LoggingSource): Intent
        {
            val intent = Intent(context, OTBackgroundLoggingService::class.java)
            intent.action = ACTION_LOG
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            intent.putExtra(INTENT_EXTRA_LOGGING_SOURCE, source.toString())
            return intent
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        //tracker_id
        if (intent != null) {
            val action = intent.action
            if (ACTION_LOG == action) {
                println("try background logging..")
                val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
                handleLogging(trackerId, intent)
            }
        }
    }

    private fun handleLogging(trackerId: String, intent: Intent) {
        val tracker = OTApplication.app.currentUser[trackerId]
        if (tracker != null) {
            startLoggingAsync(this, tracker, LoggingSource.valueOf(intent.getStringExtra(INTENT_EXTRA_LOGGING_SOURCE)), null)
        }
    }




}
