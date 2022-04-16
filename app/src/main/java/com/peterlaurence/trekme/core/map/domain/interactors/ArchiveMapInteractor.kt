package com.peterlaurence.trekme.core.map.domain.interactors

import android.net.Uri
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.dao.ArchiveMapDao
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.events.maparchive.MapArchiveEvents
import com.peterlaurence.trekme.util.ZipProgressionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Archives the map.
 */
class ArchiveMapInteractor @Inject constructor(
    private val archiveMapDao: ArchiveMapDao,
    private val mapArchiveEvents: MapArchiveEvents,
    @ApplicationScope
    private val applicationScope: CoroutineScope
) {
    private val channel = Channel<ArchiveTaskData>(1)

    init {
        /* Process archive tasks serially */
        applicationScope.launch {
            for (task in channel) {
                makeFlow(task.map, task.uri).collect {
                    mapArchiveEvents.postEvent(it)
                }
            }
        }
    }

    fun archiveMap(map: Map, uri: Uri) {
        applicationScope.launch {
            channel.send(ArchiveTaskData(map, uri))
        }
    }

    private fun makeFlow(map: Map, uri: Uri): Flow<ZipEvent> {
        return callbackFlow {
            val callback = object : ZipProgressionListener {
                private val mapName = map.name

                override fun fileListAcquired() {}

                override fun onProgress(p: Int) {
                    trySend(ZipProgressEvent(p, mapName, map.id))
                }

                override fun onZipFinished() {
                    /* Use sendBlocking instead of offer to be sure not to lose those events */
                    trySendBlocking(ZipFinishedEvent(map.id))
                    trySendBlocking(ZipCloseEvent)
                    channel.close()
                }

                override fun onZipError() {
                    trySendBlocking(ZipError)
                    cancel()
                }
            }
            launch {
                archiveMapDao.archiveMap(map, callback, uri)
            }
            awaitClose()
        }.distinctUntilChanged()
    }

    private data class ArchiveTaskData(val map: Map, val uri: Uri)

    sealed class ZipEvent
    data class ZipProgressEvent(val p: Int, val mapName: String, val mapId: Int) : ZipEvent()
    data class ZipFinishedEvent(val mapId: Int) : ZipEvent()
    object ZipError : ZipEvent()
    object ZipCloseEvent : ZipEvent()    // sent after a ZipFinishedEvent to mark as fully completed
}