@file:Suppress("BlockingMethodInNonBlockingContext")

package com.peterlaurence.trekme.features.record.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.lib.gpx.parseGpxSafely
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import com.peterlaurence.trekme.core.repositories.map.RouteRepository
import com.peterlaurence.trekme.core.repositories.recording.GpxRepository
import com.peterlaurence.trekme.data.fileprovider.TrekmeFilesProvider
import com.peterlaurence.trekme.features.common.domain.repositories.GeoRecordRepository
import com.peterlaurence.trekme.features.record.domain.interactors.DeleteRecordingInteractor
import com.peterlaurence.trekme.features.record.domain.interactors.ImportRecordingsInteractor
import com.peterlaurence.trekme.features.record.domain.interactors.RenameRecordingInteractor
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import com.peterlaurence.trekme.features.record.presentation.events.RecordEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import javax.inject.Inject


/**
 * This view-model exposes a [recordingDataFlow] flow which holds the the state of the list of
 * recordings.
 * It also responds to some events coming from UI components, such as [RecordingNameChangeEvent]
 * to trigger proper update of [recordingDataFlow].
 *
 * @since 2019/04/21
 */
@HiltViewModel
class RecordingStatisticsViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val routeRepository: RouteRepository,
    private val geoRecordRepository: GeoRecordRepository,
    private val importRecordingsInteractor: ImportRecordingsInteractor,
    private val renameRecordingInteractor: RenameRecordingInteractor,
    private val deleteRecordingInteractor: DeleteRecordingInteractor,
    private val gpxRepository: GpxRepository,
    private val eventBus: RecordEventBus,
) : ViewModel() {
    val recordingDataFlow: StateFlow<List<RecordingData>> = geoRecordRepository.recordingDataFlow

    init {
        viewModelScope.launch {
            eventBus.recordingNameChangeEvent.collect {
                onRecordingNameChangeEvent(it)
            }
        }
    }

    /**
     * Imports all [Uri]s, and notifies the user when either all imports succeeded, or one of the
     * imports failed.
     */
    fun importRecordings(uriList: List<Uri>) = viewModelScope.launch {
        importRecordingsInteractor.importRecordings(uriList)
    }

    fun getRecordingUri(recordingData: RecordingData): Uri? {
        return TrekmeFilesProvider.generateUri(recordingData.file)
    }

    private suspend fun onRecordingNameChangeEvent(event: RecordEventBus.RecordingNameChangeEvent) {
        renameRecordingInteractor.renameRecording(event.initialValue, event.newValue)
    }

    fun onRequestDeleteRecordings(recordingDataList: List<RecordingData>) = viewModelScope.launch {
        /* Remove GPX files */
        launch {
            val success = deleteRecordingInteractor.deleteRecording(recordingDataList)
            /* If only one removal failed, notify the user */
            if (!success) {
                eventBus.postRecordingDeletionFailed()
            }
        }

        /* Remove corresponding tracks on existing maps */
        launch {
            val trkIds = recordingDataList.flatMap { it.routeIds }

            /* Remove in-memory routes now */
            mapRepository.getCurrentMapList().forEach { map ->
                map.routes.value.filter { it.id in trkIds }.forEach { route ->
                    map.deleteRoute(route)
                }
            }

            /* Remove them on disk */
            mapRepository.getCurrentMapList().forEach { map ->
                routeRepository.deleteRoutesUsingId(map, trkIds)
            }
        }
    }

    fun onRequestShowElevation(recordingData: RecordingData) = viewModelScope.launch {
        /* If we already computed elevation data for this same gpx file, no need to continue */
        val gpxForElevation = gpxRepository.gpxForElevation.replayCache.firstOrNull()
        if (gpxForElevation != null && gpxForElevation.gpxFile == recordingData.file) return@launch

        /* Notify the repo that we're about to submit new data, invalidating the existing one */
        gpxRepository.resetGpxForElevation()

        withContext(Dispatchers.IO) {
            val file = recordingData.file
            val inputStream = runCatching { FileInputStream(file) }.getOrNull()
                ?: return@withContext
            val gpx = parseGpxSafely(inputStream)
            if (gpx != null) {
                gpxRepository.setGpxForElevation(gpx, file)
            }
        }
    }
}
