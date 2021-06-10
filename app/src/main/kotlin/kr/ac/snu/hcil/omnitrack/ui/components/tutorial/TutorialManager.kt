package kr.ac.snu.hcil.omnitrack.ui.components.tutorial

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import kr.ac.snu.hcil.omnitrack.BuildConfig
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

/**
 * Created by younghokim on 2017. 3. 21..
 */
class TutorialManager(val context: Context) {

    companion object {
        const val PREFERENCE_NAME = "pref_tutorial_flags"

        const val FLAG_TRACKER_LIST_ADD_TRACKER = "tracker_list_add_tracker"
    }

    val DEBUG_ALWAYS_SHOW_TUTORIAL = false

    data class TapTargetInfo(val primaryTextRes: Int, val secondaryTextRes: Int, val backgroundColor: Int, val target: View, val focalColorAlpha: Int = 255)

    data class TapTargetInfoStr(val primaryTextRes: Int, val secondaryTextRes: String, val backgroundColor: Int, val target: View, val focalColorAlpha: Int = 255)

    private fun makeFlagKey(tag: String): String {
        return "pref_tutorial_shown_flag_$tag"
    }

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    fun hasShownTutorials(tag: String): Boolean {
        return preferences.getBoolean(makeFlagKey(tag), false)
    }

    fun setTutorialFlag(tag: String, flag: Boolean) {
        preferences.edit().putBoolean(makeFlagKey(tag), flag).apply()
    }

    fun checkAndShowSequence(tag: String, closeFlagAfterClose: Boolean, activity: Activity, stopWhenTappedTarget: Boolean, sequenceList: List<TapTargetInfo>): Boolean {
        if ((DEBUG_ALWAYS_SHOW_TUTORIAL || !hasShownTutorials(tag)) && BuildConfig.SHOW_TUTORIALS && sequenceList.isNotEmpty()) {

            val list = sequenceList.asSequence().mapIndexed { index, sequence ->
                val sequenceFlagKey = "${tag}_seq_$index"
                if (!hasShownTutorials(sequenceFlagKey)) {
                    Pair(
                            MaterialTapTargetPrompt.Builder(activity)
                                    .setTarget(sequence.target)
                                    .setPrimaryText(sequence.primaryTextRes)
                                    .setSecondaryText(sequence.secondaryTextRes)
                                    .setFocalColour(Color.TRANSPARENT)
                                    //.setFocalColourAlpha(sequence.focalColorAlpha)
                                    .setCaptureTouchEventOutsidePrompt(true)
                                    .setBackgroundColour(sequence.backgroundColor), sequenceFlagKey)
                } else null
            }.filter { it != null }.map { it as Pair<MaterialTapTargetPrompt.Builder, String> }.toList()

            for (builder in list.withIndex()) {
                builder.value.first.setPromptStateChangeListener(object : MaterialTapTargetPrompt.PromptStateChangeListener {
                    private var hideByTargetTap = false
                    override fun onPromptStateChanged(prompt: MaterialTapTargetPrompt, state: Int) {
                        if (state == MaterialTapTargetPrompt.STATE_DISMISSED) {
                            setTutorialFlag(builder.value.second, true)


                            if (builder.index < list.size - 1) {
                                if (stopWhenTappedTarget && hideByTargetTap) {

                                } else list[builder.index + 1].first.show()
                            }

                            if (closeFlagAfterClose && builder.index >= list.size - 1) {
                                setTutorialFlag(tag, true)
                            }
                        }
                    }
                })
            }

            if (list.isNotEmpty()) {
                list.first().first.show()
                return true
            } else return false
        } else return false
    }

    /* easy for testing the tutorial for multiple times */
    fun checkAndShowSequenceTest(tag: String, closeFlagAfterClose: Boolean, activity: Activity, stopWhenTappedTarget: Boolean, sequenceList: List<TapTargetInfoStr>): Boolean {
       // if ((DEBUG_ALWAYS_SHOW_TUTORIAL || !hasShownTutorials(tag)) && BuildConfig.SHOW_TUTORIALS && sequenceList.isNotEmpty()) {

            val list = sequenceList.asSequence().mapIndexed { index, sequence ->
                val sequenceFlagKey = "${tag}_seq_$index"
                //if (!hasShownTutorials(sequenceFlagKey)) {
                    Pair(
                            MaterialTapTargetPrompt.Builder(activity)
                                    .setTarget(sequence.target)
                                    .setPrimaryText(sequence.primaryTextRes)
                                    .setSecondaryText(sequence.secondaryTextRes)
                                    .setFocalColour(Color.TRANSPARENT)
                                    //.setFocalColourAlpha(sequence.focalColorAlpha)
                                    .setCaptureTouchEventOutsidePrompt(true)
                                    .setBackgroundColour(sequence.backgroundColor), sequenceFlagKey)
               // } else null
            }.filter { it != null }.map { it as Pair<MaterialTapTargetPrompt.Builder, String> }.toList()

            for (builder in list.withIndex()) {
                builder.value.first.setPromptStateChangeListener(object : MaterialTapTargetPrompt.PromptStateChangeListener {
                    private var hideByTargetTap = false
                    override fun onPromptStateChanged(prompt: MaterialTapTargetPrompt, state: Int) {
                        if (state == MaterialTapTargetPrompt.STATE_DISMISSED) {
                            setTutorialFlag(builder.value.second, true)


                            if (builder.index < list.size - 1) {
                                if (stopWhenTappedTarget && hideByTargetTap) {

                                } else list[builder.index + 1].first.show()
                            }

//                            if (closeFlagAfterClose && builder.index >= list.size - 1) {
//                                setTutorialFlag(tag, true)
//                            }
                        }
                    }
                })
            }

            if (list.isNotEmpty()) {
                list.first().first.show()
                return true
            } else return false
//        } else return false
    }

    fun checkAndShowTargetPrompt(tag: String, closeFlagAfterClose: Boolean, activity: Activity, target: View, primaryText: String?, secondaryText: String?, backgroundColor: Int, focalColorAlpha: Int = 255): Boolean {
        if ((DEBUG_ALWAYS_SHOW_TUTORIAL || !hasShownTutorials(tag)) && BuildConfig.SHOW_TUTORIALS) {
            val builder = MaterialTapTargetPrompt.Builder(activity)
                    .setTarget(target)
                    .setFocalColour(Color.TRANSPARENT)
                    //.setFocalColourAlpha(focalColorAlpha)
                    .setBackgroundColour(backgroundColor)
                    .setCaptureTouchEventOutsidePrompt(true)
                    .setPromptStateChangeListener { prompt, state ->
                        if (state == MaterialTapTargetPrompt.STATE_DISMISSED) {
                            if (closeFlagAfterClose) {
                                setTutorialFlag(tag, true)
                            }
                        }
                    }
            if (primaryText != null) {
                builder.setPrimaryText(primaryText)
            }

            if (secondaryText != null) {
                builder.setSecondaryText(secondaryText)
            }

            builder.show()

            return true
        } else return false
    }

    fun checkAndShowTargetPrompt(tag: String, closeFlagAfterClose: Boolean, activity: Activity, target: View, primaryTextRes: Int, secondaryTextRes: Int, backgroundColor: Int): Boolean {
        return checkAndShowTargetPrompt(tag, closeFlagAfterClose, activity, target, activity.resources.getString(primaryTextRes), activity.resources.getString(secondaryTextRes), backgroundColor)
    }
}