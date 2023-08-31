package com.peterlaurence.trekme.features.map.domain.models

import java.util.UUID

sealed interface TrackFollowServiceState {
    data class Started(val mapId: UUID, val trackId: String): TrackFollowServiceState
    object Stopped: TrackFollowServiceState
}

data class TrackFollowServiceStopEvent(val mapId: UUID, val trackId: String)
