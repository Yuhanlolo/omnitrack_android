package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Application
import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dagger.Lazy
import io.reactivex.Maybe
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import kr.ac.snu.hcil.android.common.containers.AnyValueWithTimestamp
import kr.ac.snu.hcil.android.common.containers.AnyInputModalitywithResult
import kr.ac.snu.hcil.android.common.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTItemBuilderDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTItemBuilderFieldValueEntry
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import java.util.*

/**
 * Created by Young-Ho on 10/9/2017.
 */
class NewItemCreationViewModel(app: Application) : ItemEditionViewModelBase(app) {

    companion object {
        const val KEY_RESERVED_NEW_ITEM_ID = "reservedNewItemId"
        const val KEY_REDIRECTED_PAGE_VISIT_STATUS = "redirectedPageVisitStatus"
        const val KEY_BUILDER_ID = "builderId"
    }

    enum class RedirectedPageStatus {
        RedirectionNotSet, Completed, NotVisited, Canceled
    }

    private lateinit var itemBuilderDao: OTItemBuilderDAO
    private lateinit var builderWrapper: OTItemBuilderWrapperBase

    val redirectedPageVisitStatusObservable = BehaviorSubject.createDefault<RedirectedPageStatus>(RedirectedPageStatus.RedirectionNotSet)

    val isRedirectionSet: Boolean get() = !trackerDao.redirectUrl.isNullOrBlank()

    private var reservedNewItemId: String? = null
    fun generateNewItemId(): String {
        this.reservedNewItemId = UUID.randomUUID().toString()
        return this.reservedNewItemId!!
    }

    override fun onInject(app: OTAndroidApp) {
        getApplication<OTApp>().applicationComponent.inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_BUILDER_ID, this.itemBuilderDao.id)
        outState.putInt(KEY_REDIRECTED_PAGE_VISIT_STATUS, this.redirectedPageVisitStatusObservable.value?.ordinal
                ?: RedirectedPageStatus.RedirectionNotSet.ordinal)
        outState.putString(KEY_RESERVED_NEW_ITEM_ID, reservedNewItemId)
        refreshDaoValues()
        var highestBuilderEntryId = realm.where(OTItemBuilderFieldValueEntry::class.java).max("id")?.toLong()
                ?: 0L
        itemBuilderDao.data.forEach {
            if (!it.isManaged && it.id == -1L) {
                it.id = ++highestBuilderEntryId
            }
        }
        realm.executeTransaction {
            realm.insertOrUpdate(itemBuilderDao)
        }
    }

    fun init(trackerId: String, metadata: JsonObject?, savedInstanceState: Bundle?) {
        if (init(trackerId)) {

            if (!trackerDao.redirectUrl.isNullOrBlank()) {
                redirectedPageVisitStatusObservable.onNextIfDifferAndNotNull(RedirectedPageStatus.NotVisited)
            } else {
                redirectedPageVisitStatusObservable.onNextIfDifferAndNotNull(RedirectedPageStatus.RedirectionNotSet)
            }

            //val builderDaoResult = if (loadPendingItemBuilder) dbManager.get().getItemBuilderQuery(trackerDao._id!!, OTItemBuilderDAO.HOLDER_TYPE_INPUT_FORM, realm).findFirst() else null

            val storedBuilderDao = if (savedInstanceState != null && savedInstanceState.containsKey(KEY_BUILDER_ID)) {
                //there is a pending itemBuilder.

                println("restore the itemBuilder")
                realm.where(OTItemBuilderDAO::class.java).equalTo("id", savedInstanceState.getLong(KEY_BUILDER_ID)).findFirst()
            } else null

            if (storedBuilderDao == null) {
                val newBuilderId = System.currentTimeMillis()
                realm.executeTransactionIfNotIn {
                    val newBuilderDao = realm.createObject(OTItemBuilderDAO::class.java, newBuilderId)
                    newBuilderDao.tracker = trackerDao
                    newBuilderDao.holderType = OTItemBuilderDAO.HOLDER_TYPE_INPUT_FORM
                    newBuilderDao.serializedMetadata = metadata?.toString()
                    this.itemBuilderDao = realm.copyFromRealm(newBuilderDao)
                }
            } else {
                this.itemBuilderDao = realm.copyFromRealm(storedBuilderDao)
            }

            println("metadata at start (NewItemCreationViewModel): ${this.itemBuilderDao.serializedMetadata}")

            this.builderWrapper = OTItemBuilderWrapperBase(this.itemBuilderDao, getApplication())

            for (key in this.builderWrapper.keys) {
                val value = this.builderWrapper.getValueInformationOf(key)
                if (value != null) {
                    setValueOfAttribute(key, value)
                }
            }

            if (savedInstanceState == null) {
                isBusy = true
                subscriptions.add(
                        this.builderWrapper.makeAutoCompleteObservable(realmProvider, this).subscribe({ (fieldLocalId, valueWithTimestamp) ->
                            setValueOfAttribute(fieldLocalId, valueWithTimestamp)
                        }, {

                        }, {
                            isBusy = false
                        })
                )
            } else {
                this.reservedNewItemId = savedInstanceState.getString(KEY_RESERVED_NEW_ITEM_ID)
                this.redirectedPageVisitStatusObservable.onNextIfDifferAndNotNull(RedirectedPageStatus.values()[savedInstanceState.getInt(KEY_REDIRECTED_PAGE_VISIT_STATUS)])
            }
        }
    }

    override fun setValueOfAttribute(fieldLocalId: String, valueWithTimestamp: AnyValueWithTimestamp) {
        itemBuilderDao.setValue(fieldLocalId, valueWithTimestamp)
        super.setValueOfAttribute(fieldLocalId, valueWithTimestamp)
    }

    override fun isViewModelsDirty(): Boolean {
        for (viewModel in currentAttributeViewModelList) {
            val match = itemBuilderDao.data.find { it.fieldLocalId == viewModel.fieldLocalId }
            if (match != null) {
                val vmValue = viewModel.value
                if (vmValue == null) {
                    return true
                } else {
                    if ((vmValue.timestamp != match.timestamp) || (vmValue.value != match.serializedValue?.let { TypeStringSerializationHelper.deserialize(it) })) {
                        return true
                    }
                }
            } else return viewModel.value != null
        }

        return false
    }

    private fun refreshDaoValues() {
        var metaStr = ""
        realm.executeTransaction {
            currentAttributeViewModelList.forEach { attrViewModel ->
                val value = attrViewModel.value
                itemBuilderDao.setValue(attrViewModel.fieldLocalId, value)

                val inputmodalityListStr = attrViewModel.inputModalityList.toString()
                println ("metadata recordList after refreshing values (NewItemCreationViewModel): $inputmodalityListStr")
                if(!inputmodalityListStr.equals("[]")){
                    val size = inputmodalityListStr.length
                    val nextMeta = inputmodalityListStr.substring(1, size - 1) + ","
                    if (!metaStr.equals(nextMeta))
                        metaStr += nextMeta
                }
            }
        }

        var gson = Gson()
        val obj = gson.fromJson(itemBuilderDao.serializedMetadata, JsonObject::class.java).toString()
        val originalMetaSize = obj.length
        val newMetaSize = metaStr.length

        if(!metaStr.equals(""))
            itemBuilderDao.serializedMetadata = obj.substring(0, originalMetaSize - 1) + ", \"ModalityChoice\":[" + metaStr.substring(0, newMetaSize - 1) + "]}"

        println ("metadata after passing to itemBuilderDao (NewItemCreationViewModel): ${itemBuilderDao.serializedMetadata}")

    }

    override fun modifyMetadata(handler: (metadata: JsonObject) -> Unit) {
        builderWrapper.modifyMetadata(realm, handler)
    }

    override fun applyEditingToDatabase(): Maybe<String> {
        if (isValid) {
            return isBusyObservable.filter { !it }.firstOrError().flatMapMaybe {
                refreshDaoValues()
                val item = builderWrapper.saveToItem(null, ItemLoggingSource.Manual)
                item._id = reservedNewItemId

                return@flatMapMaybe Maybe.fromSingle<String>(dbManager.get().saveItemObservable(item, false, null, realm).map { it.second })
                        .doAfterSuccess {
                            reservedNewItemId = null
                            syncItemToServer()
                            OTApp.logger.writeSystemLog("Logged new item in the page. metadata: \n${item.serializedMetadata}", "NewItemCreationViewModel")
                        }
            }
        } else return Maybe.just(null)
    }


    fun clearHistory() {
        subscriptions.add(
                isBusyObservable.filter { !it }.subscribe { isBusy ->
                    realm.executeTransaction { transactionRealm ->
                        val daos = transactionRealm.where(OTItemBuilderDAO::class.java).equalTo("id", itemBuilderDao.id).findAll()
                        daos.forEach {
                            it.data.deleteAllFromRealm()
                        }
                        daos.deleteAllFromRealm()
                    }
                    println("removed item builder.")
                    isValid = false
                }
        )
    }


    fun setResultOfRedirectedPage(result: RedirectedPageStatus) {
        this.redirectedPageVisitStatusObservable.onNextIfDifferAndNotNull(result)
    }

}