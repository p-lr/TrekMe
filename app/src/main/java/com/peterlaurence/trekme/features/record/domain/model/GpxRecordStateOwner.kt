package com.peterlaurence.trekme.features.record.domain.model

import kotlinx.coroutines.flow.StateFlow

interface GpxRecordStateOwner {
    val gpxRecordState: StateFlow<GpxRecordState>

    fun setServiceState(state: GpxRecordState)
}