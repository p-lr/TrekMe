package com.peterlaurence.trekme.core.map.domain.interactors

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.dao.ArchiveMapDao
import com.peterlaurence.trekme.util.ZipProgressionListener
import com.peterlaurence.trekme.util.stackTraceAsString
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.OutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Archives the map.
 *
 * Creates a zip file named with this [Map] name and the date. This file is placed in the
 * parent folder of the [Map].
 * Beware that this is a blocking call and should be executed from inside a background thread.
 */
class ArchiveMapInteractor @Inject constructor(
    private val archiveMapDao: ArchiveMapDao,
    private val app: Application
) {
    fun archiveMap(map: Map, uri: Uri): Flow<ZipEvent>? {
        val docFile = DocumentFile.fromTreeUri(app.applicationContext, uri)
        return if (docFile != null && docFile.isDirectory) {
            val newFileName: String = map.generateNewNameWithDate() + ".zip"
            val newFile = docFile.createFile("application/zip", newFileName)
            if (newFile != null) {
                val uriZip = newFile.uri
                runCatching {
                    val out: OutputStream = app.contentResolver.openOutputStream(uriZip)
                        ?: return null
                    /* The underlying task which writes into the stream is responsible for closing this stream. */
                    zipProgressFlow(map, out).distinctUntilChanged()
                }.onFailure {
                    Log.e(this.javaClass.name, it.stackTraceAsString())
                }.getOrNull()
            } else null
        } else null
    }

    private fun Map.generateNewNameWithDate(): String {
        val date = Date()
        val dateFormat: DateFormat = SimpleDateFormat("dd\\MM\\yyyy-HH:mm:ss", Locale.ENGLISH)
        return name + "-" + dateFormat.format(date)
    }

    @ExperimentalCoroutinesApi
    private fun zipProgressFlow(map: Map, outputStream: OutputStream): Flow<ZipEvent> = callbackFlow {
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
            archiveMapDao.archiveMap(map, callback, outputStream)
        }
        awaitClose()
    }

    sealed class ZipEvent
    data class ZipProgressEvent(val p: Int, val mapName: String, val mapId: Int): ZipEvent()
    data class ZipFinishedEvent(val mapId: Int): ZipEvent()
    object ZipError : ZipEvent()
    object ZipCloseEvent: ZipEvent()    // sent after a ZipFinishedEvent to mark as fully completed
}