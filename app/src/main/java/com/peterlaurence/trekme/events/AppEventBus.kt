package com.peterlaurence.trekme.events

import com.peterlaurence.trekme.core.billing.data.model.BillingParams
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Application-wide event-bus.
 *
 * @since 2020/10/31
 */
class AppEventBus {
    private val _genericMessageEvents = MutableSharedFlow<GenericMessage>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val genericMessageEvents = _genericMessageEvents.asSharedFlow()

    fun postMessage(msg: GenericMessage) {
        _genericMessageEvents.tryEmit(msg)
    }

    /**********************************************************************************************/

    private val _requestBackgroundLocationSignal = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requestBackgroundLocationSignal = _requestBackgroundLocationSignal.asSharedFlow()

    fun requestBackgroundLocation(rationale: String) = _requestBackgroundLocationSignal.tryEmit(rationale)

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

    /**********************************************************************************************/

    private val _openDrawerFlow = MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)
    val openDrawerFlow = _openDrawerFlow.asSharedFlow()

    fun openDrawer() = _openDrawerFlow.tryEmit(Unit)

    /**********************************************************************************************/

    private val _navigateToFlow = MutableSharedFlow<NavDestination>(0, 1, BufferOverflow.DROP_OLDEST)
    val navigateFlow = _navigateToFlow.asSharedFlow()

    fun navigateTo(dest: NavDestination) = _navigateToFlow.tryEmit(dest)

    enum class NavDestination {
        Shop, MapList, MapCreation, TrailSearch
    }
}