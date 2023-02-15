package com.peterlaurence.trekme.features.record.presentation.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.record.app.service.GpxRecordService
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
    private val appEventBus: AppEventBus,
    private val app: Application,
    private val settings: Settings,
) : ViewModel() {
    val status: StateFlow<GpxRecordState> = gpxRecordEvents.serviceState

    val disableBatteryOptSignal = Channel<Unit>(1)
    val showLocalisationRationale = Channel<Unit>(1)

    private var isButtonEnabled = true

    fun onStartStopClicked() {
        if (!isButtonEnabled) return

        viewModelScope.launch {
            isButtonEnabled = false
            when (gpxRecordEvents.serviceState.value) {
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
            when (gpxRecordEvents.serviceState.value) {
                GpxRecordState.STOPPED -> { /* Nothing to do */
                }
                GpxRecordState.STARTED, GpxRecordState.RESUMED -> gpxRecordEvents.pauseRecording()
                GpxRecordState.PAUSED -> gpxRecordEvents.resumeRecording()
            }
        }
    }

    private suspend fun startRecording() {
        /* Check location service. If disabled, no need to go further. */
        if (!isLocationEnabled()) {
            val msg = WarningMessage(
                title = app.applicationContext.getString(R.string.warning_title),
                msg = app.applicationContext.getString(R.string.location_disabled_warning)
            )
            appEventBus.postMessage(msg)
            return
        }

        /* Check battery optimization, and inform the user if needed */
        if (isBatteryOptimized()) {
            disableBatteryOptSignal.send(Unit)
        }

        /* Start the service */
        val intent = Intent(app, GpxRecordService::class.java)
        app.startService(intent)

        /* The background location permission is asked after the rationale is closed. But it doesn't
         * matter that the recording is already started - it works even when the permission is
         * granted during the recording. */
        if (!isBackgroundLocationGranted() && settings.isShowingLocationRationale().first()) {
            showLocalisationRationale.send(Unit)
        } else {
            /* If the disclaimer is discarded, ask for the permission anyway */
            requestBackgroundLocationPerm()
        }
    }

    /**
     * Check the battery optimization.
     */
    private fun isBatteryOptimized(): Boolean {
        val pm = app.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val name = app.applicationContext.packageName
        return !pm.isIgnoringBatteryOptimizations(name)
    }

    private fun isBackgroundLocationGranted(): Boolean {
        if (Build.VERSION.SDK_INT < 29) return true
        val permissionLocation = ActivityCompat.checkSelfPermission(
            app.applicationContext,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        return permissionLocation == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val lm =
            app.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    fun requestBackgroundLocationPerm() {
        appEventBus.requestBackgroundLocation()
    }

    fun onIgnoreLocationRationale() = viewModelScope.launch {
        settings.discardLocationDisclaimer()
    }
}

const val START_STOP_DISABLE_TIMEOUT = 2000