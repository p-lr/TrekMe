package com.peterlaurence.trekme.features.map.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.record.app.service.GpxRecordService
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordState
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordStateOwner
import com.peterlaurence.trekme.util.android.isBackgroundLocationGranted
import com.peterlaurence.trekme.util.android.isBatteryOptimized
import com.peterlaurence.trekme.util.android.isLocationEnabled
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Expose to the activity and fragment/views the state of the [GpxRecordService].
 *
 * @since 27/04/2019
 */
@HiltViewModel
class GpxRecordServiceViewModel @Inject constructor(
    private val gpxRecordEvents: GpxRecordEvents,
    gpxRecordStateOwner: GpxRecordStateOwner,
    private val appEventBus: AppEventBus,
    private val app: Application,
) : ViewModel() {
    val status: StateFlow<GpxRecordState> = gpxRecordStateOwner.gpxRecordState

    val ackBatteryOptSignal = Channel<Unit>(1)

    private val _events = Channel<Event>(1)
    val events = _events.receiveAsFlow()

    private var isButtonEnabled = true

    fun onStartStopClicked() {
        if (!isButtonEnabled) return

        viewModelScope.launch {
            isButtonEnabled = false
            when (status.value) {
                GpxRecordState.STOPPED -> startRecording()
                GpxRecordState.STARTED, GpxRecordState.PAUSED, GpxRecordState.RESUMED -> {
                    gpxRecordEvents.stopRecording()
                }
            }
            delay(START_STOP_DISABLE_TIMEOUT.toLong())
            isButtonEnabled = true
        }
    }

    fun onPauseResumeClicked() {
        viewModelScope.launch {
            when (status.value) {
                GpxRecordState.STOPPED -> { /* Nothing to do */
                }

                GpxRecordState.STARTED, GpxRecordState.RESUMED -> gpxRecordEvents.pauseRecording()
                GpxRecordState.PAUSED -> gpxRecordEvents.resumeRecording()
            }
        }
    }

    private suspend fun startRecording() {
        /* Check location service. If disabled, no need to go further. */
        if (!isLocationEnabled(app.applicationContext)) {
            val msg = WarningMessage(
                title = app.applicationContext.getString(R.string.warning_title),
                msg = app.applicationContext.getString(R.string.location_disabled_warning)
            )
            appEventBus.postMessage(msg)
            return
        }

        /* Check battery optimization, and inform the user if needed */
        if (isBatteryOptimized(app.applicationContext)) {
            _events.send(Event.DisableBatteryOptSignal)
            /* Wait for the user to take action before continuing */
            ackBatteryOptSignal.receive()
        }

        if (!isBackgroundLocationGranted(app.applicationContext)) {
            val request = AppEventBus.BackgroundLocationRequest(R.string.background_location_rationale_gpx_recording)
            appEventBus.requestBackgroundLocation(request)

            val granted = request.result.receiveAsFlow().firstOrNull() ?: false
            if (!granted) {
                appEventBus.postMessage(
                    WarningMessage(
                        title = app.getString(R.string.warning_title),
                        msg = app.getString(R.string.background_location_gpx_recording_failure)
                    )
                )
                return
            }
        }

        /* Start service */
        val intent = Intent(app, GpxRecordService::class.java)
        app.startService(intent)
    }

    sealed interface Event {
        object DisableBatteryOptSignal : Event
    }
}

const val START_STOP_DISABLE_TIMEOUT = 2000