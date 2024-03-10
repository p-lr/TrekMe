package com.peterlaurence.trekme.events.gpspro

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class GpsProEvents {
    private val _requestBluetoothPermission = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requestBluetoothPermissionFlow = _requestBluetoothPermission.asSharedFlow()

    fun requestBluetoothPermission() = _requestBluetoothPermission.tryEmit(Unit)

    /**********************************************************************************************/

    private val _bluetoothPermissionResult = MutableSharedFlow<Boolean>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val bluetoothPermissionResultFlow = _bluetoothPermissionResult.asSharedFlow()

    fun postBluetoothPermissionResult(granted: Boolean) = _bluetoothPermissionResult.tryEmit(granted)

    /**********************************************************************************************/

    private val _nmeaSentencesFlow = MutableSharedFlow<String>(extraBufferCapacity = 50, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nmeaSentencesFlow = _nmeaSentencesFlow.asSharedFlow()

    fun postNmeaSentence(sentence: String) = _nmeaSentencesFlow.tryEmit(sentence)
}