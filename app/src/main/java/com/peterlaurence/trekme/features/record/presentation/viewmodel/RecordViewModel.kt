package com.peterlaurence.trekme.features.record.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.PowerManager
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.georecord.domain.interactors.GeoRecordInteractor
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.core.map.BoundingBox
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.intersects
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.common.domain.interactors.georecord.ImportGeoRecordInteractor
import com.peterlaurence.trekme.service.GpxRecordService
import com.peterlaurence.trekme.service.event.GpxFileWriteEvent
import com.peterlaurence.trekme.features.record.presentation.events.RecordEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.*
import javax.inject.Inject

/**
 * The view-model associated with the record fragment.
 *
 * @since 2020/04/16
 */
@HiltViewModel
class RecordViewModel @Inject constructor(
    private val geoRecordInteractor: GeoRecordInteractor,
    private val importGeoRecordInteractor: ImportGeoRecordInteractor,
    private val getMapInteractor: GetMapInteractor,
    private val app: Application,
    private val settings: Settings,
    private val gpxRecordEvents: GpxRecordEvents,
    private val eventBus: RecordEventBus,
    private val appEventBus: AppEventBus,
) : ViewModel() {
    init {
        viewModelScope.launch {
            gpxRecordEvents.gpxFileWriteEvent.collect {
                onGpxFileWriteEvent(it)
            }
        }

        viewModelScope.launch {
            eventBus.mapSelectedEvent.collect { (mapId, recordId) ->
                onMapSelectedForRecord(mapId, recordId)
            }
        }

        eventBus.startGpxRecordingSignal.map {
            onRequestStartEvent()
        }.launchIn(viewModelScope)

        eventBus.stopGpxRecordingSignal.map {
            gpxRecordEvents.stopRecording()
        }.launchIn(viewModelScope)

        eventBus.pauseGpxRecordingSignal.map {
            gpxRecordEvents.pauseRecording()
        }.launchIn(viewModelScope)

        eventBus.resumeGpxRecordingSignal.map {
            gpxRecordEvents.resumeRecording()
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            eventBus.locationDisclaimerClosedSignal.collect {
                requestBackgroundLocationPerm()
            }
        }

        viewModelScope.launch {
            eventBus.discardLocationDisclaimerSignal.collect {
                settings.discardLocationDisclaimer()
            }
        }
    }

    /**
     * Whenever a [GpxFileWriteEvent] is emitted, import the gpx track in all maps which intersects
     * the [BoundingBox] of the gpx track.
     */
    private suspend fun onGpxFileWriteEvent(event: GpxFileWriteEvent) {
        val boundingBox = event.boundingBox ?: return

        var importCount = 0
        supervisorScope {
            getMapInteractor.getMapList().forEach { map ->
                launch {
                    if (map.intersects(boundingBox)) {
                        /* Import the new route */
                        val result = importGeoRecordInteractor.applyGeoRecordFileToMap(event.gpxFile, map)
                        appEventBus.postGeoRecordImportResult(result)
                        if (result is GeoRecordImportResult.GeoRecordImportOk && result.newRouteCount >= 1) {
                            importCount++
                        }
                    }
                }
            }
        }
        if (importCount > 0) {
            val msg = app.applicationContext.getString(R.string.automatic_import_feedback, importCount)
            appEventBus.postMessage(StandardMessage(msg))
        }
    }


    private fun onMapSelectedForRecord(mapId: UUID, recordId: UUID) {
        val map = getMapInteractor.getMap(mapId) ?: return

        val uri = geoRecordInteractor.getRecordUri(recordId) ?: return

        viewModelScope.launch {
            importGeoRecordInteractor.applyGeoRecordUriToMap(uri, app.contentResolver, map).let {
                /* Once done, notify the rest of the app */
                appEventBus.postGeoRecordImportResult(it)
            }
        }
    }

    private suspend fun onRequestStartEvent() {
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
            eventBus.disableBatteryOpt()
        }

        /* Start the service */
        val intent = Intent(app, GpxRecordService::class.java)
        app.startService(intent)

        /* The background location permission is asked after the dialog is closed. But it doesn't
         * matter that the recording is already started - it works even when the permission is
         * granted during the recording. */
        if (settings.isShowingLocationDisclaimer().first()) {
            eventBus.showLocationDisclaimer()
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

    private fun isLocationEnabled(): Boolean {
        val lm = app.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    private fun requestBackgroundLocationPerm() {
        appEventBus.requestBackgroundLocation()
    }
}