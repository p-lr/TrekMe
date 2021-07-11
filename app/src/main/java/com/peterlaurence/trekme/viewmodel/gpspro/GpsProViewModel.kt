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

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    init {
        if (bluetoothAdapter == null) {
            bluetoothState = BtNotSupported
        } else {
            startUpProcedure(bluetoothAdapter)
        }
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
        pairedDevices?.forEach { device ->
            println("${device.name} : ${device.address}")
        }
    }
}

sealed class BluetoothState
object BtNotSupported : BluetoothState()
object BtDisabled : BluetoothState()
object Searching : BluetoothState()
data class PairedDeviceList(val deviceList: List<String>) : BluetoothState()