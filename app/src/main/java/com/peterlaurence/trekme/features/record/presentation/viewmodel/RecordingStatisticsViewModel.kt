@file:Suppress("BlockingMethodInNonBlockingContext")

package com.peterlaurence.trekme.features.record.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.georecord.domain.interactors.GeoRecordInteractor
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import com.peterlaurence.trekme.core.repositories.map.RouteRepository
import com.peterlaurence.trekme.features.common.domain.repositories.RecordingDataRepository
import com.peterlaurence.trekme.features.record.domain.interactors.ImportRecordingsInteractor
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import com.peterlaurence.trekme.features.record.presentation.events.RecordEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    recordingDataRepository: RecordingDataRepository,
    private val geoRecordInteractor: GeoRecordInteractor,
    private val importRecordingsInteractor: ImportRecordingsInteractor,
    private val eventBus: RecordEventBus,
) : ViewModel() {
    val recordingDataFlow: StateFlow<List<RecordingData>> = recordingDataRepository.recordingDataFlow

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
        return geoRecordInteractor.getRecordUri(recordingData.id)
    }

    private suspend fun onRecordingNameChangeEvent(event: RecordEventBus.RecordingNameChangeEvent) {
        geoRecordInteractor.rename(event.id, event.newValue)
    }

    fun onRequestDeleteRecordings(recordingDataList: List<RecordingData>) = viewModelScope.launch {
        /* Remove GPX files */
        launch {
            val success = geoRecordInteractor.delete(recordingDataList.map { it.id })
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
}