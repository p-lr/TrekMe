package com.peterlaurence.trekme.main.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.map.domain.models.intersects
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import com.peterlaurence.trekme.features.record.app.service.event.NewExcursionEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

/**
 * Whenever a [NewExcursionEvent] is emitted, import the gpx track in all maps which intersects
 * the [BoundingBox] of the gpx track.
 */
@HiltViewModel
class RecordingEventHandlerViewModel @Inject constructor(
    val gpxRecordEvents: GpxRecordEvents,
    private val mapExcursionInteractor: MapExcursionInteractor,
    private val getMapInteractor: GetMapInteractor,
    private val appEventBus: AppEventBus,
    @ApplicationContext
    private val appContext: Context
) : ViewModel() {

    fun onNewExcursionEvent(event: NewExcursionEvent) = viewModelScope.launch {
        val boundingBox = event.boundingBox ?: return@launch

        var importCount = 0
        supervisorScope {
            getMapInteractor.getMapList().forEach { map ->
                launch {
                    if (map.intersects(boundingBox)) {
                        mapExcursionInteractor.createExcursionRef(map, event.excursionId)
                        importCount++
                    }
                }
            }
        }
        if (importCount > 0) {
            val msg = appContext.getString(R.string.automatic_import_feedback, importCount)
            appEventBus.postMessage(StandardMessage(msg))
        }
    }
}