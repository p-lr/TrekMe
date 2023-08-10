package com.peterlaurence.trekme.main.eventhandler

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.map.domain.models.intersects
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.common.domain.interactors.ImportGeoRecordInteractor
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.features.record.app.service.event.NewExcursionEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class RecordingEventHandler(
    private val lifecycle: Lifecycle,
    private val gpxRecordEvents: GpxRecordEvents,
    private val importGeoRecordInteractor: ImportGeoRecordInteractor,
    private val getMapInteractor: GetMapInteractor,
    private val onImportDone: (importCount: Int) -> Unit
) {
    init {
        lifecycle.coroutineScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                gpxRecordEvents.newExcursionEvent.collect {
                    onNewExcursionEvent(it)
                }
            }
        }
    }

    /**
     * Whenever a [NewExcursionEvent] is emitted, import the gpx track in all maps which intersects
     * the [BoundingBox] of the gpx track.
     */
    private suspend fun onNewExcursionEvent(event: NewExcursionEvent) {
        val boundingBox = event.boundingBox ?: return

        var importCount = 0
        supervisorScope {
            getMapInteractor.getMapList().forEach { map ->
                launch {
                    if (map.intersects(boundingBox)) {
                        /* Import the new route */
                        val result =
                            importGeoRecordInteractor.applyGeoRecordToMap(event.geoRecord, map)
                        if (result is GeoRecordImportResult.GeoRecordImportOk && result.newRouteCount >= 1) {
                            importCount++
                        }
                    }
                }
            }
        }
        if (importCount > 0) {
            onImportDone(importCount)
        }
    }
}