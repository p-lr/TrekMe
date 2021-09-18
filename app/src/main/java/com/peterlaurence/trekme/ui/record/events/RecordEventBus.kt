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

    /**********************************************************************************************/

    private val _showLocationDisclaimerSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val showLocationDisclaimerSignal = _showLocationDisclaimerSignal.asSharedFlow()

    fun showLocationDisclaimer() = _showLocationDisclaimerSignal.tryEmit(Unit)

    private val _locationDisclaimerClosedSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val locationDisclaimerClosedSignal = _locationDisclaimerClosedSignal.asSharedFlow()

    fun closeLocationDisclaimer() = _locationDisclaimerClosedSignal.tryEmit(Unit)

    private val _discardLocationDisclaimerSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val discardLocationDisclaimerSignal = _discardLocationDisclaimerSignal.asSharedFlow()

    fun discardLocationDisclaimer() = _discardLocationDisclaimerSignal.tryEmit(Unit)

    /**********************************************************************************************/

    private val _startGpxRecordingSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val startGpxRecordingSignal = _startGpxRecordingSignal.asSharedFlow()

    fun startGpxRecording() = _startGpxRecordingSignal.tryEmit(Unit)

    /**********************************************************************************************/

    private val _stopGpxRecordingSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val stopGpxRecordingSignal = _stopGpxRecordingSignal.asSharedFlow()

    fun stopGpxRecording() = _stopGpxRecordingSignal.tryEmit(Unit)

    /**********************************************************************************************/

    private val _pauseGpxRecordingSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val pauseGpxRecordingSignal = _pauseGpxRecordingSignal.asSharedFlow()

    fun pauseGpxRecording() = _pauseGpxRecordingSignal.tryEmit(Unit)

    /**********************************************************************************************/

    private val _resumeGpxRecordingSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val resumeGpxRecordingSignal = _resumeGpxRecordingSignal.asSharedFlow()

    fun resumeGpxRecording() = _resumeGpxRecordingSignal.tryEmit(Unit)

    /**********************************************************************************************/

    private val _disableBatteryOptSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val disableBatteryOptSignal = _disableBatteryOptSignal.asSharedFlow()

    fun disableBatteryOpt() = _disableBatteryOptSignal.tryEmit(Unit)
}