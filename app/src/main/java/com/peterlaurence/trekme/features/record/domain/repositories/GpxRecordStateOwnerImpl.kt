package com.peterlaurence.trekme.features.record.domain.repositories

import com.peterlaurence.trekme.features.record.domain.model.GpxRecordState
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordStateOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class GpxRecordStateOwnerImpl @Inject constructor(): GpxRecordStateOwner {
    private val _gpxRecordState = MutableStateFlow(GpxRecordState.STOPPED)
    override val gpxRecordState : StateFlow<GpxRecordState> = _gpxRecordState.asStateFlow()

    /**
     * Should only by used by the service.
     */
    override fun setServiceState(state: GpxRecordState) {
        _gpxRecordState.value = state
    }
}