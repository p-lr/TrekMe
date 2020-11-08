package com.peterlaurence.trekme.core.events

import com.peterlaurence.trekme.core.track.TrackImporter
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Application-wide event-bus.
 *
 * @author P.Laurence on 31/10/2020
 */
class AppEventBus {
    private val _genericMessageEvents = MutableSharedFlow<GenericMessage>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val genericMessageEvents = _genericMessageEvents.asSharedFlow()

    fun postMessage(msg: GenericMessage) {
        _genericMessageEvents.tryEmit(msg)
    }

    /**********************************************************************************************/

    private val _requestBackgroundLocationSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requestBackgroundLocationSignal = _requestBackgroundLocationSignal.asSharedFlow()

    fun requestBackgroundLocation() = _requestBackgroundLocationSignal.tryEmit(Unit)

    /**********************************************************************************************/

    private val _gpxImportEvent = MutableSharedFlow<TrackImporter.GpxImportResult>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val gpxImportEvent = _gpxImportEvent.asSharedFlow()

    fun postGpxImportResult(event: TrackImporter.GpxImportResult) = _gpxImportEvent.tryEmit(event)
}