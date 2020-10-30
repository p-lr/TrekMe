package com.peterlaurence.trekme.repositories.recording

import com.peterlaurence.trekme.util.gpx.model.TrackPoint
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

    fun addTrackPoint(trackPoint: TrackPoint) {
        _liveRoute.tryEmit(LiveRoutePoint(trackPoint))
    }

    fun reset() {
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
