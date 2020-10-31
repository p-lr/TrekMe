package com.peterlaurence.trekme.repositories.recording

import com.peterlaurence.trekme.service.event.GpxFileWriteEvent
import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class GpxRecordRepository {
    private val _liveRoute = MutableSharedFlow<LiveRouteEvent>(replay = Int.MAX_VALUE)
    val liveRouteFlow = _liveRoute.asSharedFlow()

    /* The status started / stopped of the service */
    private val _serviceState = MutableStateFlow(false)
    val serviceState = _serviceState.asStateFlow()

    /* The service listens to this signal. Upon reception of this signal, the service creates a
     * gpx file and stop itself after notifying its state. */
    private val _stopRecordingSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val stopRecordingSignal = _stopRecordingSignal.asSharedFlow()

    private val _gpxFileWriteEvent = MutableSharedFlow<GpxFileWriteEvent>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val gpxFileWriteEvent = _gpxFileWriteEvent.asSharedFlow()

    fun produceGpxFileWriteEvent(event: GpxFileWriteEvent) {
        _gpxFileWriteEvent.tryEmit(event)
    }

    fun stopRecording() {
        _stopRecordingSignal.tryEmit(Unit)
    }

    fun addTrackPoint(trackPoint: TrackPoint) {
        _liveRoute.tryEmit(LiveRoutePoint(trackPoint))
    }

    fun resetLiveRoute() {
        _liveRoute.resetReplayCache()
        _liveRoute.tryEmit(LiveRouteStop)
    }

    /**
     * Should only by used by the service.
     */
    fun setServiceState(started: Boolean) {
        _serviceState.value = started
    }
}

sealed class LiveRouteEvent
data class LiveRoutePoint(val pt: TrackPoint) : LiveRouteEvent()
object LiveRouteStop : LiveRouteEvent()
