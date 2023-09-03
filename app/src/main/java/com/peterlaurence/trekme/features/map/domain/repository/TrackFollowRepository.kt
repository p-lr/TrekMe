package com.peterlaurence.trekme.features.map.domain.repository

import com.peterlaurence.trekme.features.map.domain.core.TrackVicinityVerifier
import com.peterlaurence.trekme.features.map.domain.models.TrackFollowServiceState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackFollowRepository @Inject constructor() {
    val serviceState = MutableStateFlow<TrackFollowServiceState>(TrackFollowServiceState.Stopped)

    val serviceData = Channel<ServiceData>(CONFLATED)

    data class ServiceData(val verifier: TrackVicinityVerifier, val mapId: UUID, val trackId: String)
}

