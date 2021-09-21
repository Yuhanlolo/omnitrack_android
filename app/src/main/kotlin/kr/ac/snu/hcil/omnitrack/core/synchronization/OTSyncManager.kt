package kr.ac.snu.hcil.omnitrack.core.synchronization

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.di.global.ServerFullSync
import kr.ac.snu.hcil.omnitrack.core.di.global.ServerSyncOneShot
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationClientSideAPI
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import kr.ac.snu.hcil.omnitrack.core.workers.OTSynchronizationWorker
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 9. 27..
 */
@Singleton
class OTSyncManager @Inject constructor(
        private val context: Context,
        private val syncQueueDbHelper: SyncQueueDbHelper,
        private val syncClient: Lazy<ISynchronizationClientSideAPI>,
        private val syncServer: Lazy<ISynchronizationServerSideAPI>,
        private val eventLogger: Lazy<IEventLogger>,
        @ServerSyncOneShot private val oneShotRequestProvider: Provider<OneTimeWorkRequest>,
        @ServerFullSync private val periodicRequestProvider: Provider<PeriodicWorkRequest>
) {

    fun registerSyncQueue(type: ESyncDataType, direction: SyncDirection, reserveService: Boolean = true, ignoreDirtyFlags: Boolean = false) {
        syncQueueDbHelper.insertNewEntry(type, direction, System.currentTimeMillis(), ignoreDirtyFlags)
        if (reserveService) {
            reserveSyncServiceNow()
        }
    }

    fun clearSynchronizationOnDevice() {
        WorkManager.getInstance(context).let {
            it.cancelUniqueWork(OTSynchronizationWorker.TAG)
            it.cancelAllWorkByTag(OTSynchronizationWorker.TAG)
        }
        syncQueueDbHelper.purgeEntries(null)
    }

    fun queueFullSync(direction: SyncDirection = SyncDirection.BIDIRECTIONAL, ignoreFlags: Boolean) {
        ESyncDataType.values().forEach { type ->
            syncQueueDbHelper.insertNewEntry(type, direction, System.currentTimeMillis(), ignoreFlags)
        }
    }

    @Synchronized
    fun reserveSyncServiceNow() {
        println("reserve data synchronization from server.")
        WorkManager.getInstance(context).enqueueUniqueWork(OTSynchronizationWorker.TAG, ExistingWorkPolicy.REPLACE, oneShotRequestProvider.get())
    }

    @Synchronized
    fun reservePeriodicSyncWorker() {
        if (WorkManager.getInstance(context).getWorkInfosByTag(OTSynchronizationWorker.TAG).get().isEmpty()) {
            WorkManager.getInstance(context).enqueue(periodicRequestProvider.get())
        }
    }

    @Synchronized
    fun refreshWorkers() {
        WorkManager.getInstance(context).let {
            it.cancelUniqueWork(OTSynchronizationWorker.TAG)
            it.cancelAllWorkByTag(OTSynchronizationWorker.TAG)
        }

        reserveSyncServiceNow()
        reservePeriodicSyncWorker()
    }

 /*handling the quotation marks issue in text input*/
    fun handleQuotationMarks (input: List<Pair<ESyncDataType, List<String>>>): List<Pair<ESyncDataType, List<String>>>{
        var res = listOfNotNull<Pair<ESyncDataType, List<String>>>()

        for (it in input){
            var strList: List<String> = listOf()
            for (str in it.second){
                  var temp = str
//                println ("sync send dirty rows to server: temp $temp")
//                if (!str.equals("")){
//                   if(str.contains("\"in the zone.\"\"")){
//                       temp = str.replace("\"in the zone.\"\"", "\\\"in the zone.\\\"\"")
//                   }
//                } "ModalityChoice":

                var subStr = str
                if (str.contains("\"ModalityChoice\":")){
                    var subStr = str.substring(str.indexOf("\"ModalityChoice\":"))
                    temp = str.substring(0,str.indexOf("\"ModalityChoice\":"))

                    var tempList: Array<String> = subStr.split("},").toTypedArray()
                    var temp2 = ""
                    var count = 0
                    var size = tempList.size

                    for (t in tempList){
                        var tStr = t
                        var endIndex = tStr.length-1
                        var endSign = "\""
                        if(tStr.last().toString().equals("]")){
                            endIndex = tStr.length-3
                            endSign = "\"}]"
                        }

                        if (tStr.contains("\"input\": \"")){
                            var inputContent = tStr.substring(tStr.indexOf("\"input\": \"") + 10, endIndex)
                            //println ("sync send dirty rows to server: tStr inputContent $inputContent, endIndex char: ${tStr.get(endIndex)}")

                            if (inputContent.contains("\n")){
                                inputContent = tStr.substring(tStr.indexOf("\"input\": \"") + 10, endIndex + 1)
                                inputContent = inputContent.replace("\n", " ")
                                tStr = tStr.substring(0, tStr.indexOf("\"input\": \"")) + "\"input\": \"" + inputContent + endSign

                                println ("sync send dirty rows to server: find a new line! $tStr, inputContent: $inputContent")
                            }

                            if (inputContent.contains("\"")){
                                inputContent = inputContent.replace("\"", "\\\"")
                                tStr = tStr.substring(0, tStr.indexOf("\"input\": \"")) + "\"input\": \"" + inputContent + endSign

                                //println ("sync send dirty rows to server: tStr after $tStr")
                            }

                        }

                        if (count != size -1)
                            temp2+= tStr + "},"
                        else
                            temp2+= tStr

                        count ++
                    }
                    temp += temp2
                }

                strList += temp
            }

            res += Pair(it.first, strList)
            //println ("sync send dirty rows to server: first${it.first}, second: ${strList}")
        }

        return res

    }


    fun makeSynchronizationTask(batchData: SyncQueueDbHelper.AggregatedSyncQueue): Completable {
        val downDirection = Completable.defer {
            val dbLatestTimestamps = batchData.data.filter { it.second.contains(SyncDirection.DOWNLOAD) }
                    .map { (type, _) ->
                        Pair(type, syncClient.get().getLatestSynchronizedServerTimeOf(type))
                    }.toTypedArray()
            if (dbLatestTimestamps.isNotEmpty()) {
                syncServer.get().getRowsSynchronizedAfter(*dbLatestTimestamps).flatMapCompletable { serverItemsMap ->
                    println(serverItemsMap)
                    Completable.merge(
                            serverItemsMap.map { (type, serverItems) ->
                                if (serverItems.isNotEmpty()) {
                                    syncClient.get().applyServerRowsToSync(type, serverItems.toList())
                                } else {
                                    Completable.complete()
                                }
                            }
                    )
                }
            } else {
                return@defer Completable.complete()
            }
        }

        val upDirection = Completable.defer {
            val downEntries = batchData.data.filter { it.second.contains(SyncDirection.UPLOAD) }.toTypedArray()
            println("start sync download from server: ${downEntries.map { it.first }.joinToString(",")}")
            if (downEntries.isEmpty()) {
                return@defer Completable.complete()
            }

            return@defer Single.zip(downEntries.map { (type, direction, ignoreFlags) -> syncClient.get().getDirtyRowsToSync(type, ignoreFlags).map { Pair(type, it) } }) { clientDirtyRowsArray ->
                clientDirtyRowsArray.map { it as Pair<ESyncDataType, List<String>> }
            }.flatMap {
                var res = handleQuotationMarks(it)
                println("sync send dirty rows to server: $res")
                syncServer.get().postDirtyRows(*res.map { entry -> ISynchronizationServerSideAPI.DirtyRowBatchParameter(entry.first, entry.second.toTypedArray()) }.toTypedArray())
            }.flatMapCompletable { result ->
                Completable.merge(result.map { entry -> syncClient.get().setTableSynchronizationFlags(entry.key, entry.value.toList()) })
            }
        }

        return downDirection.andThen(upDirection)
    }
}