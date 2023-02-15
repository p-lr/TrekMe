package com.peterlaurence.trekme.features.record.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.georecord.domain.interactors.GeoRecordInteractor
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.map.domain.models.intersects
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.common.domain.interactors.georecord.ImportGeoRecordInteractor
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.features.record.app.service.event.GpxFileWriteEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.*
import javax.inject.Inject


@HiltViewModel
class RecordViewModel @Inject constructor(
    private val geoRecordInteractor: GeoRecordInteractor,
    private val importGeoRecordInteractor: ImportGeoRecordInteractor,
    private val getMapInteractor: GetMapInteractor,
    private val app: Application,
    private val gpxRecordEvents: GpxRecordEvents,
    private val appEventBus: AppEventBus,
) : ViewModel() {

    init {
        viewModelScope.launch {
            gpxRecordEvents.gpxFileWriteEvent.collect {
                onGpxFileWriteEvent(it)
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
                        val result =
                            importGeoRecordInteractor.applyGeoRecordFileToMap(event.gpxFile, map)
                        appEventBus.postGeoRecordImportResult(result)
                        if (result is GeoRecordImportResult.GeoRecordImportOk && result.newRouteCount >= 1) {
                            importCount++
                        }
                    }
                }
            }
        }
        if (importCount > 0) {
            val msg =
                app.applicationContext.getString(R.string.automatic_import_feedback, importCount)
            appEventBus.postMessage(StandardMessage(msg))
        }
    }

    fun importRecordInMap(mapId: UUID, recordId: UUID) {
        val map = getMapInteractor.getMap(mapId) ?: return

        val uri = geoRecordInteractor.getRecordUri(recordId) ?: return

        viewModelScope.launch {
            importGeoRecordInteractor.applyGeoRecordUriToMap(uri, app.contentResolver, map).let {
                /* Once done, notify the rest of the app */
                appEventBus.postGeoRecordImportResult(it)
            }
        }
    }
}