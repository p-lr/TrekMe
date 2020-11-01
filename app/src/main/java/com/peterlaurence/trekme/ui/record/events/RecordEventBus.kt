package com.peterlaurence.trekme.ui.record.events

import com.peterlaurence.trekme.ui.record.components.events.RecordingNameChangeEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class RecordEventBus {
    private val _mapSelectedEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val mapSelectedEvent = _mapSelectedEvent.asSharedFlow()

    fun setMapSelected(mapId: Int) {
        _mapSelectedEvent.tryEmit(mapId)
    }

    /**********************************************************************************************/

    private val _recordingNameChangeEvent =
            MutableSharedFlow<RecordingNameChangeEvent>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val recordingNameChangeEvent = _recordingNameChangeEvent.asSharedFlow()

    fun postRecordingNameChange(initialValue: String, newValue: String) {
        _recordingNameChangeEvent.tryEmit(RecordingNameChangeEvent(initialValue, newValue))
    }

    /**********************************************************************************************/

    private val _recordingDeletionFailedSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val recordingDeletionFailedSignal = _recordingDeletionFailedSignal.asSharedFlow()

    fun postRecordingDeletionFailed() = _recordingDeletionFailedSignal.tryEmit(Unit)
}