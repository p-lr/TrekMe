package com.peterlaurence.trekme.features.record.domain.model

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ElevationStateOwner {
    val elevationState: StateFlow<ElevationState>
    val events: SharedFlow<ElevationEvent>
}
