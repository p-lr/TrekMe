package com.peterlaurence.trekme.features.maplist.presentation.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.models.CalibrationMethod
import com.peterlaurence.trekme.core.map.domain.interactors.SaveMapInteractor
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import com.peterlaurence.trekme.features.maplist.presentation.events.*
import com.peterlaurence.trekme.util.ZipProgressionListener
import com.peterlaurence.trekme.util.makeThumbnail
import com.peterlaurence.trekme.util.stackTraceAsString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

/**
 * The view-model for the MapSettingsFragment.
 *
 * @author P.Laurence on 14/08/20
 */
@HiltViewModel
class MapSettingsViewModel @Inject constructor(
        val app: Application,
        private val mapLoader: MapLoader,
        private val saveMapInteractor: SaveMapInteractor,
        private val mapRepository: MapRepository
) : ViewModel() {

    private val _zipEvents = MutableLiveData<ZipEvent>()
    val zipEvents: LiveData<ZipEvent> = _zipEvents

    private val _mapImageImportEvents = MutableSharedFlow<MapImageImportResult>(0, 1, BufferOverflow.DROP_OLDEST)
    val mapImageImportEvents = _mapImageImportEvents.asSharedFlow()

    val map: Map?
        get() = mapRepository.getSettingsMap()

    /**
     * Changes the thumbnail of a [Map].
     * Compression of the file defined by the [uri] is done off UI-thread.
     */
    fun setMapImage(mapId: Int, uri: Uri) = viewModelScope.launch {
        val map = mapLoader.getMap(mapId) ?: return@launch

        try {
            val thumbnail = withContext(Dispatchers.Default) {
                val outputStream = map.imageOutputStream
                if (outputStream != null) {
                    makeThumbnail(uri, app.contentResolver, map.thumbnailSize, outputStream)
                } else null
            }
            if (thumbnail != null) {
                map.image = thumbnail
                saveMapInteractor.saveMap(map)
                _mapImageImportEvents.tryEmit(MapImageImportResult(true))
            } else {
                _mapImageImportEvents.tryEmit(MapImageImportResult(false))
            }
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceAsString())
            _mapImageImportEvents.tryEmit(MapImageImportResult(false))
        }
    }

    /**
     * Start zipping a map and write the zip archive to the directory defined by the provided [uri].
     * Internally uses a [Flow] which only emits distinct events. Those events are listened by the
     * main activity, and not the [MapSettingsFragment], because the user might leave this view ;
     * we want to reliably inform the user when this task is finished.
     */
    fun archiveMap(map: Map, uri: Uri) = viewModelScope.launch {
        val docFile = DocumentFile.fromTreeUri(app.applicationContext, uri)
        if (docFile != null && docFile.isDirectory) {
            val newFileName: String = map.generateNewNameWithDate() + ".zip"
            val newFile = docFile.createFile("application/zip", newFileName)
            if (newFile != null) {
                val uriZip = newFile.uri
                try {
                    val out: OutputStream = app.contentResolver.openOutputStream(uriZip)
                            ?: return@launch
                    /* The underlying task which writes into the stream is responsible for closing this stream. */
                    zipProgressFlow(map.id, out).distinctUntilChanged().collect {
                        _zipEvents.value = it
                    }
                } catch (e: IOException) {
                    Log.e(TAG, e.stackTraceAsString())
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    private fun zipProgressFlow(mapId: Int, outputStream: OutputStream): Flow<ZipEvent> = callbackFlow {
        val map = mapLoader.getMap(mapId) ?: return@callbackFlow

        val callback = object : ZipProgressionListener {
            private val mapName = map.name

            override fun fileListAcquired() {}

            override fun onProgress(p: Int) {
                trySend(ZipProgressEvent(p, mapName, mapId))
            }

            override fun onZipFinished() {
                /* Use sendBlocking instead of offer to be sure not to lose those events */
                trySendBlocking(ZipFinishedEvent(mapId))
                trySendBlocking(ZipCloseEvent)
                channel.close()
            }

            override fun onZipError() {
                trySendBlocking(ZipError)
                cancel()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            map.zip(callback, outputStream)
        }
        awaitClose()
    }

    fun renameMap(map: Map, newName: String) {
        viewModelScope.launch {
            mapLoader.renameMap(map, newName)
            saveMapInteractor.saveMap(map)
        }
    }

    fun setCalibrationPointsNumber(map: Map, numberStr: String?) {
        when (numberStr) {
            "2" -> map.calibrationMethod = CalibrationMethod.SIMPLE_2_POINTS
            "3" -> map.calibrationMethod = CalibrationMethod.CALIBRATION_3_POINTS
            "4" -> map.calibrationMethod = CalibrationMethod.CALIBRATION_4_POINTS
            else -> map.calibrationMethod = CalibrationMethod.SIMPLE_2_POINTS
        }
        saveMapAsync(map)
    }

    fun setProjection(map: Map, projectionName: String?): Boolean {
        return if (projectionName != null) {
            mapLoader.mutateMapProjection(map, projectionName)
        } else {
            map.projection = null
            true
        }.also {
            saveMapAsync(map)
        }
    }

    private fun saveMapAsync(map: Map) {
        viewModelScope.launch {
            saveMapInteractor.saveMap(map)
        }
    }

    suspend fun computeMapSize(map: Map): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val size = map.directory!!.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            withContext(Dispatchers.Main) {
                map.setSizeInBytes(size)
                saveMapAsync(map)
            }
            size
        }.getOrNull()
    }
}

private const val TAG = "MapSettingsViewModel.kt"