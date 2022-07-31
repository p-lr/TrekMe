package com.peterlaurence.trekme.features.common.domain.model

import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import kotlinx.coroutines.flow.StateFlow

interface RecordingDataStateOwner {
    val recordingDataFlow: StateFlow<List<RecordingData>>
}