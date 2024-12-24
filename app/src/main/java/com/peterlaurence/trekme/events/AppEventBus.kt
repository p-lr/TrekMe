package com.peterlaurence.trekme.events

import com.peterlaurence.trekme.core.billing.data.model.BillingParams
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Application-wide event-bus.
 *
 * @since 2020/10/31
 */
class AppEventBus {
    private val _genericMessageEvents = Channel<GenericMessage>(Channel.BUFFERED)
    val genericMessageEvents = _genericMessageEvents.receiveAsFlow()

    fun postMessage(msg: GenericMessage) {
        _genericMessageEvents.trySend(msg)
    }

    /**********************************************************************************************/

    private val _requestBackgroundLocationSignal = MutableSharedFlow<BackgroundLocationRequest>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requestBackgroundLocationSignal = _requestBackgroundLocationSignal.asSharedFlow()

    fun requestBackgroundLocation(request: BackgroundLocationRequest) = _requestBackgroundLocationSignal.tryEmit(request)

    val backgroundLocationResult = Channel<Boolean>(1)

    data class BackgroundLocationRequest(val rationaleId: Int)

    /**********************************************************************************************/

    private val _requestBluetoothEnableFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requestBluetoothEnableFlow = _requestBluetoothEnableFlow.asSharedFlow()

    fun requestBluetoothEnable() = _requestBluetoothEnableFlow.tryEmit(Unit)

    /**********************************************************************************************/

    private val _bluetoothEnableFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val bluetoothEnabledFlow = _bluetoothEnableFlow.asSharedFlow()

    fun bluetoothEnabled(enabled: Boolean) = _bluetoothEnableFlow.tryEmit(enabled)

    /**********************************************************************************************/

    private val _requestNotificationPermFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requestNotificationPermFlow = _requestNotificationPermFlow.asSharedFlow()

    fun requestNotificationPermission() = _requestNotificationPermFlow.tryEmit(Unit)

    /**********************************************************************************************/

    private val _requestNearbyWifiDevicesPermFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requestNearbyWifiDevicesPermFlow = _requestNearbyWifiDevicesPermFlow.asSharedFlow()

    fun requestNearbyWifiDevicesPerm() = _requestNearbyWifiDevicesPermFlow.tryEmit(Unit)


    /**********************************************************************************************/

    private val _billingFLow = MutableSharedFlow<BillingParams>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val billingFlow = _billingFLow.asSharedFlow()

    fun startBillingFlow(billingParams: BillingParams) = _billingFLow.tryEmit(billingParams)

}