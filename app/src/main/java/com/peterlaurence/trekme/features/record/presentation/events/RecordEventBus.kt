package com.peterlaurence.trekme.features.record.presentation.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class RecordEventBus {
    private val _recordingDeletionFailedSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val recordingDeletionFailedSignal = _recordingDeletionFailedSignal.asSharedFlow()

    fun postRecordingDeletionFailed() = _recordingDeletionFailedSignal.tryEmit(Unit)
}