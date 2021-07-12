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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class GpsProViewModel @Inject constructor(
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
            startUpProcedure(bluetoothAdapter)
        }
    }

    fun onHostSelected() {
        isHostSelected = !isHostSelected
    }

    fun onBtDeviceSelection(device: BluetoothDeviceStub) {
        device.isActive = !device.isActive
    }

    private fun startUpProcedure(bluetoothAdapter: BluetoothAdapter) {
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

    private fun queryPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        bluetoothState = PairedDeviceList(pairedDevices?.map {
            BluetoothDeviceStub(it.name, it.address)
        } ?: listOf())
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