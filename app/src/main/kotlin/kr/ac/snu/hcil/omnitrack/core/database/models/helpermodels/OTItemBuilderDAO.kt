package kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.android.common.containers.AnyInputModalitywithResult
import kr.ac.snu.hcil.android.common.containers.AnyValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * Created by Young-Ho on 10/9/2017.
 */
open class OTItemBuilderDAO : RealmObject() {

    companion object {
        const val HOLDER_TYPE_INPUT_FORM = 0
        const val HOLDER_TYPE_TRIGGER = 1
        const val HOLDER_TYPE_SERVICE = 2
    }

    @PrimaryKey
    var id: Long = 0
    var createdAt: Long = System.currentTimeMillis()
    var tracker: OTTrackerDAO? = null

    @Index
    var holderType: Int = 0

    var data = RealmList<OTItemBuilderFieldValueEntry>()

    var serializedMetadata: String? = null

    fun setValue(fieldLocalId: String, value: AnyValueWithTimestamp?) {
        val match = data.find { it.fieldLocalId == fieldLocalId }
        if (match != null) {
            if (value == null) {
                if (match.isManaged)
                    match.deleteFromRealm()
                data.remove(match)
            } else {
                match.serializedValue = value.value?.let { TypeStringSerializationHelper.serialize(it) }
                match.timestamp = value.timestamp ?: 0
            }
        } else {
            if (value != null) {
                val newEntryDao = OTItemBuilderFieldValueEntry()
                newEntryDao.fieldLocalId = fieldLocalId
                newEntryDao.serializedValue = value.value?.let { TypeStringSerializationHelper.serialize(it) }
                newEntryDao.timestamp = value.timestamp ?: 0
                data.add(newEntryDao)
            }
        }
    }
}

open class OTItemBuilderFieldValueEntry : RealmObject() {
    @PrimaryKey
    var id: Long = -1

    var fieldLocalId: String? = null
    var serializedValue: String? = null
    var timestamp: Long = System.currentTimeMillis()
    //var serializedMetadata: String? = null
}