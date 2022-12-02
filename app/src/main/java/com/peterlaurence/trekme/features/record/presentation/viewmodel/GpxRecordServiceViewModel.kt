package com.peterlaurence.trekme.features.record.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.record.app.service.GpxRecordService
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordState
import com.peterlaurence.trekme.features.record.presentation.events.RecordEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Expose to the activity and fragment/views the state of the [GpxRecordService].
 *
 * @author P.Laurence on 27/04/2019
 */
@HiltViewModel
class GpxRecordServiceViewModel @Inject constructor(
    private val gpxRecordEvents: GpxRecordEvents,
    private val recordEvents: RecordEventBus
) : ViewModel() {
    val status: StateFlow<GpxRecordState> = gpxRecordEvents.serviceState

    private var isButtonEnabled = true

    fun onStartStopClicked() {
        if (!isButtonEnabled) return

        viewModelScope.launch {
            isButtonEnabled = false
            when (gpxRecordEvents.serviceState.value) {
                GpxRecordState.STOPPED -> recordEvents.startGpxRecording()
                GpxRecordState.STARTED -> recordEvents.stopGpxRecording()
                GpxRecordState.PAUSED -> recordEvents.stopGpxRecording()
                GpxRecordState.RESUMED -> recordEvents.stopGpxRecording()
            }
            delay(START_STOP_DISABLE_TIMEOUT.toLong())
            isButtonEnabled = true
        }
    }

    fun onPauseResumeClicked() {
        viewModelScope.launch {
            when (gpxRecordEvents.serviceState.value) {
                GpxRecordState.STOPPED -> { /* Nothing to do */
                }
                GpxRecordState.STARTED -> recordEvents.pauseGpxRecording()
                GpxRecordState.PAUSED -> recordEvents.resumeGpxRecording()
                GpxRecordState.RESUMED -> recordEvents.pauseGpxRecording()
            }
        }
    }
}

const val START_STOP_DISABLE_TIMEOUT = 2000