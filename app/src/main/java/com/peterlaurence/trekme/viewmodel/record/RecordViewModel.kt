package com.peterlaurence.trekme.viewmodel.record

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.events.StandardMessage
import com.peterlaurence.trekme.core.map.BoundingBox
import com.peterlaurence.trekme.core.map.intersects
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.core.track.id
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.repositories.recording.GpxRepository
import com.peterlaurence.trekme.service.GpxRecordService
import com.peterlaurence.trekme.service.event.GpxFileWriteEvent
import com.peterlaurence.trekme.ui.record.events.RecordEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

/**
 * The view-model associated with the record fragment.
 *
 * @author P.Laurence on 16/04/20
 */
@HiltViewModel
class RecordViewModel @Inject constructor(
        private val gpxRepository: GpxRepository,
        private val trackImporter: TrackImporter,
        private val app: Application,
        private val settings: Settings,
        private val gpxRecordEvents: GpxRecordEvents,
        private val eventBus: RecordEventBus,
        private val appEventBus: AppEventBus,
        private val mapLoader: MapLoader
) : ViewModel() {
    private var recordingsSelected = listOf<RecordingData>()

    init {
        viewModelScope.launch {
            gpxRecordEvents.gpxFileWriteEvent.collect {
                onGpxFileWriteEvent(it)
            }
        }

        viewModelScope.launch {
            eventBus.mapSelectedEvent.collect {
                onMapSelectedForRecord(it)
            }
        }

        viewModelScope.launch {
            eventBus.startGpxRecordingSignal.collect {
                onRequestStartEvent()
            }
        }

        viewModelScope.launch {
            eventBus.stopGpxRecordingSignal.collect {
                gpxRecordEvents.stopRecording()
            }
        }

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
        val gpx = event.gpx

        val boundingBox = gpx.metadata?.bounds?.let {
            BoundingBox(it.minLat, it.maxLat, it.minLon, it.maxLon)
        } ?: return

        var importCount = 0
        supervisorScope {
            mapLoader.maps.forEach { map ->
                launch {
                    if (map.intersects(boundingBox) == true) {
                        /* Import the new route */
                        val result = trackImporter.applyGpxToMap(gpx, map, mapLoader)
                        if (result is TrackImporter.GpxImportResult.GpxImportOk && result.newRouteCount >= 1) {
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

    fun setSelectedRecordings(recordings: List<RecordingData>) {
        recordingsSelected = recordings
    }

    /**
     * The business logic of parsing a GPX file.
     */
    private fun onMapSelectedForRecord(mapId: Int) {
        val map = mapLoader.getMap(mapId) ?: return

        val recordingData = recordingsSelected.firstOrNull() ?: return
        val recording = gpxRepository.recordings?.firstOrNull { it.id() == recordingData.id }
                ?: return

        viewModelScope.launch {
            trackImporter.applyGpxFileToMap(recording, map, mapLoader).let {
                /* Once done, notify the rest of the app */
                appEventBus.postGpxImportResult(it)
            }
        }
    }

    private fun onRequestStartEvent() {
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
        if (settings.isShowingLocationDisclaimer()) {
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

    private fun requestBackgroundLocationPerm() {
        appEventBus.requestBackgroundLocation()
    }
}