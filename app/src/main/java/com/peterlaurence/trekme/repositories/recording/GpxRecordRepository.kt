package com.peterlaurence.trekme.repositories.recording

import com.peterlaurence.trekme.core.track.TrackStatistics
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

    fun addTrackPoint(trackPoint: TrackPoint) {
        _liveRoute.tryEmit(LiveRoutePoint(trackPoint))
    }

    fun resetLiveRoute() {
        _liveRoute.resetReplayCache()
        _liveRoute.tryEmit(LiveRouteStop)
    }

    /**********************************************************************************************/

    /* Status started / stopped of the service */
    private val _serviceState = MutableStateFlow(false)
    val serviceState = _serviceState.asStateFlow()

    /**
     * Should only by used by the service.
     */
    fun setServiceState(started: Boolean) {
        _serviceState.value = started
    }

    /**********************************************************************************************/

    /* The service listens to this signal. Upon reception of this signal, the service creates a
     * gpx file and stop itself after notifying its state. */
    private val _stopRecordingSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val stopRecordingSignal = _stopRecordingSignal.asSharedFlow()

    fun stopRecording() {
        _stopRecordingSignal.tryEmit(Unit)
    }

    /**********************************************************************************************/

    private val _gpxFileWriteEvent = MutableSharedFlow<GpxFileWriteEvent>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val gpxFileWriteEvent = _gpxFileWriteEvent.asSharedFlow()

    fun postGpxFileWriteEvent(event: GpxFileWriteEvent) {
        _gpxFileWriteEvent.tryEmit(event)
    }

    /**********************************************************************************************/

    private val _trackStatisticsEvent = MutableSharedFlow<TrackStatistics>(1, 0, BufferOverflow.DROP_OLDEST)
    val trackStatisticsEvent = _trackStatisticsEvent.asSharedFlow()

    fun postTrackStatisticsEvent(event: TrackStatistics) {
        _trackStatisticsEvent.tryEmit(event)
    }
}

sealed class LiveRouteEvent
data class LiveRoutePoint(val pt: TrackPoint) : LiveRouteEvent()
object LiveRouteStop : LiveRouteEvent()
