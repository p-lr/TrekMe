package com.peterlaurence.trekme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.service.GpxRecordService
import com.peterlaurence.trekme.service.GpxRecordState
import com.peterlaurence.trekme.ui.record.events.RecordEventBus
import com.peterlaurence.trekme.util.CircularArray
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
    val lastState: GpxRecordState
        get() = states.lastOrNull() ?: status.value

    private var states = CircularArray<GpxRecordState>(1)

    init {
        status.map {
            states.add(it)
        }.launchIn(viewModelScope)
    }

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
        if (!isButtonEnabled) return

        viewModelScope.launch {
            isButtonEnabled = false
            when (gpxRecordEvents.serviceState.value) {
                GpxRecordState.STOPPED -> { /* Nothing to do */
                }
                GpxRecordState.STARTED -> recordEvents.pauseGpxRecording()
                GpxRecordState.PAUSED -> recordEvents.resumeGpxRecording()
                GpxRecordState.RESUMED -> recordEvents.pauseGpxRecording()
            }
            delay(START_STOP_DISABLE_TIMEOUT.toLong())
            isButtonEnabled = true
        }
    }
}

const val START_STOP_DISABLE_TIMEOUT = 2000