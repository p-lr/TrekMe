package com.peterlaurence.trekme.repositories.recording

import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class GpxRecordRepository {
    private val _liveRoute = MutableSharedFlow<LiveRouteEvent>(replay = Int.MAX_VALUE)
    val liveRouteFlow = _liveRoute.asSharedFlow()

    fun addTrackPoint(trackPoint: TrackPoint) {
        _liveRoute.tryEmit(LiveRoutePoint(trackPoint))
    }

    fun reset() {
        _liveRoute.resetReplayCache()
        _liveRoute.tryEmit(LiveRouteStop)
    }
}

sealed class LiveRouteEvent
data class LiveRoutePoint(val pt: TrackPoint) : LiveRouteEvent()
object LiveRouteStop : LiveRouteEvent()
