package com.peterlaurence.trekme.features.common.domain.model

import kotlinx.coroutines.flow.StateFlow

interface RecordingDataStateOwner {
    val recordingDataFlow: StateFlow<RecordingsState>
}