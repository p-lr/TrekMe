package com.peterlaurence.trekme.viewmodel.gpspro

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.model.InternalGps
import com.peterlaurence.trekme.core.model.LocationProducerBtInfo
import com.peterlaurence.trekme.core.model.LocationProducerInfo
import com.peterlaurence.trekme.core.settings.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GpsProViewModel @Inject constructor(
        private val settings: Settings,
        val app: Application,
        private val appEventBus: AppEventBus
) : ViewModel() {
    var bluetoothState by mutableStateOf<BluetoothState>(Searching)
    var isHostSelected by mutableStateOf(false)

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    init {
        if (bluetoothAdapter == null) {
            bluetoothState = BtNotSupported
        } else {
            viewModelScope.launch {
                startUpProcedure(bluetoothAdapter)
            }
        }

        /* When we're notified of a new active location producer, we update our internal states
         * accordingly. */
        settings.getLocationProducerInfo().map { info ->
            /* Internal GPS */
            isHostSelected = info is InternalGps

            /* Bluetooth devices */
            val btState = bluetoothState
            if (btState is PairedDeviceList) {
                btState.deviceList.forEach {
                    it.setIsActive(info)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onHostSelected() = viewModelScope.launch {
        settings.setLocationProducerInfo(InternalGps)
    }

    fun onBtDeviceSelection(device: BluetoothDeviceStub) = viewModelScope.launch {
        settings.setLocationProducerInfo(LocationProducerBtInfo(device.name, device.address))
    }

    private suspend fun startUpProcedure(bluetoothAdapter: BluetoothAdapter) {
        if (!bluetoothAdapter.isEnabled) {
            appEventBus.bluetoothEnabledFlow.map { enabled ->
                if (enabled) {
                    queryPairedDevices()
                } else {
                    println("Can't query paired devices")
                }
            }.launchIn(viewModelScope)

            appEventBus.requestBluetoothEnable()
        } else {
            queryPairedDevices()
        }
    }

    /**
     * When we get the list of paired devices, we check if one of them is the active location
     * producer.
     */
    private suspend fun queryPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val producerInfo = settings.getLocationProducerInfo().first()
        bluetoothState = PairedDeviceList(pairedDevices?.map {
            BluetoothDeviceStub(it.name, it.address).apply {
                setIsActive(producerInfo)
            }
        } ?: listOf())
    }

    /**
     * Use the mac address to uniquely identify a producer.
     */
    private fun BluetoothDeviceStub.setIsActive(producerInfo: LocationProducerInfo) {
        isActive = if (producerInfo is LocationProducerBtInfo) {
            address == producerInfo.macAddress
        } else {
            false
        }
    }
}

data class BluetoothDeviceStub(val name: String, val address: String) {
    var isActive by mutableStateOf(false)
}

sealed class BluetoothState
object BtNotSupported : BluetoothState()
object BtDisabled : BluetoothState()
object Searching : BluetoothState()
data class PairedDeviceList(val deviceList: List<BluetoothDeviceStub>) : BluetoothState()