@file:Suppress("BlockingMethodInNonBlockingContext")

package com.peterlaurence.trekme.features.record.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import com.peterlaurence.trekme.features.common.domain.model.RecordingDataStateOwner
import com.peterlaurence.trekme.features.common.domain.model.RecordingsAvailable
import com.peterlaurence.trekme.features.common.domain.model.RecordingsState
import com.peterlaurence.trekme.features.common.domain.interactors.ImportRecordingsInteractor
import com.peterlaurence.trekme.features.record.domain.interactors.RecordingInteractor
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * This view-model exposes a [recordingDataFlow] flow which holds the the state of the list of
 * recordings.
 *
 * @since 2019/04/21
 */
@HiltViewModel
class RecordingStatisticsViewModel @Inject constructor(
    recordingDataStateOwner: RecordingDataStateOwner,
    private val recordingInteractor: RecordingInteractor,
    private val importRecordingsInteractor: ImportRecordingsInteractor,
    private val mapExcursionInteractor: MapExcursionInteractor
) : ViewModel() {

    val recordingDataFlow: StateFlow<RecordingsState> = recordingDataStateOwner.recordingDataFlow

    private val _isTrackSharePending = MutableStateFlow(false)
    val isTrackSharePending = _isTrackSharePending.asStateFlow()

    private val _eventChannel = Channel<RecordingEvent>(1)
    val eventChannel = _eventChannel.receiveAsFlow()

    private val recordingDeletionFailureChannel = Channel<Unit>(1)
    val recordingDeletionFailureFlow = recordingDeletionFailureChannel.receiveAsFlow()

    init {
        /* Emit an event when there's a new element in the list */
        viewModelScope.launch {
            recordingDataFlow.filterIsInstance<RecordingsAvailable>().scan(0) { s, l ->
                if (l.recordings.size > s) {
                    _eventChannel.send(RecordingEvent.NewRecording)
                }
                l.recordings.size
            }.collect()
        }
    }

    /**
     * Imports all [Uri]s, and notifies the user when either all imports succeeded, or one of the
     * imports failed.
     */
    fun importRecordings(uriList: List<Uri>) = viewModelScope.launch {
        importRecordingsInteractor.importRecordings(uriList)
    }

    fun shareRecordings(recordingsIds: List<String>) = viewModelScope.launch {
        _isTrackSharePending.value = true
        val uris = recordingsIds.mapNotNull { id ->
            recordingInteractor.getGeoRecordUri(id)
        }
        _isTrackSharePending.value = false

        if (uris.isNotEmpty()) {
            _eventChannel.send(RecordingEvent.ShareRecordings(uris))
        } else {
            _eventChannel.send(RecordingEvent.ShareRecordingFailure)
        }
    }

    fun renameRecording(id: String, newName: String) {
        viewModelScope.launch {
            recordingInteractor.rename(id, newName)
        }
    }

    fun onRequestDeleteRecordings(recordingDataList: List<RecordingData>) = viewModelScope.launch {
        val ids = recordingDataList.map { it.id }
        /* Remove recordings */
        launch {
            val success = recordingInteractor.delete(ids)
            /* If only one removal failed, notify the user */
            if (!success) {
                recordingDeletionFailureChannel.send(Unit)
            }
        }

        val excursionIds = recordingDataList.map { it.id }
        /* Remove corresponding excursions on existing maps */
        launch {
            excursionIds.forEach { id ->
                mapExcursionInteractor.removeExcursionOnMaps(id)
            }
        }
    }
}

sealed interface RecordingEvent {
    data object NewRecording : RecordingEvent
    data class ShareRecordings(val uris: List<Uri>) : RecordingEvent
    data object ShareRecordingFailure : RecordingEvent
}
