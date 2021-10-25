package com.peterlaurence.trekme.viewmodel.gpspro

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.model.InternalGps
import com.peterlaurence.trekme.core.model.LocationProducerBtInfo
import com.peterlaurence.trekme.core.model.LocationProducerInfo
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.repositories.gpspro.GpsProDiagnosisRepo
import com.peterlaurence.trekme.ui.gpspro.events.GpsProEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GpsProViewModel @Inject constructor(
    private val settings: Settings,
    app: Application,
    private val appEventBus: AppEventBus,
    private val gpsProEvents: GpsProEvents,
    private val diagnosisRepo: GpsProDiagnosisRepo,
) : ViewModel() {
    var bluetoothState by mutableStateOf<BluetoothState>(Searching)
    var isHostSelected by mutableStateOf(false)
    var isDiagnosisRunning = diagnosisRepo.diagnosisRunningStateFlow

    private val bluetoothAdapter: BluetoothAdapter? =
        (app.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    init {
        /* If we had to request bluetooth connect perm, handle the result here */
        gpsProEvents.bluetoothPermissionResultFlow.map { granted ->
            if (granted) start() else {
                bluetoothState = BtConnectPermNotGranted
            }
        }.launchIn(viewModelScope)

        if (bluetoothAdapter == null) {
            bluetoothState = BtNotSupported
        } else {
            /* For Android 12 and onwards, request runtime BLUETOOTH_CONNECT permission */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                    app.applicationContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gpsProEvents.requestBluetoothPermission()
            } else {
                start()
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

    private fun start() {
        viewModelScope.launch {
            if (bluetoothAdapter != null) {
                startUpProcedure(bluetoothAdapter)
            }
        }
    }

    fun onHostSelected() = viewModelScope.launch {
        settings.setLocationProducerInfo(InternalGps)
    }

    fun onBtDeviceSelection(device: BluetoothDeviceStub) = viewModelScope.launch {
        settings.setLocationProducerInfo(LocationProducerBtInfo(device.name, device.address))
    }

    fun onShowBtDeviceSettings() {
        gpsProEvents.requestShowBtDeviceSettingsFragment()
    }

    private suspend fun startUpProcedure(bluetoothAdapter: BluetoothAdapter) {
        if (!bluetoothAdapter.isEnabled) {
            appEventBus.bluetoothEnabledFlow.map { enabled ->
                if (enabled) {
                    queryPairedDevices()
                } else {
                    bluetoothState = BtDisabled
                    onHostSelected()
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

    fun generateDiagnosis() {
        /* The name of the current selected device. At this point, it shouldn't be null */
        val name = bluetoothState.selectedDevice?.name ?: return
        diagnosisRepo.generateDiagnosis(name)
    }

    fun cancelDiagnosis() = diagnosisRepo.cancelDiagnosis()

    fun saveDiagnosis() = diagnosisRepo.saveDiagnosis()
}

data class BluetoothDeviceStub(val name: String, val address: String) {
    var isActive by mutableStateOf(false)
}

val BluetoothState.selectedDevice: BluetoothDeviceStub?
    get() = when (this) {
        is PairedDeviceList -> deviceList.firstOrNull { it.isActive }
        else -> null
    }

sealed class BluetoothState
object BtNotSupported : BluetoothState()
object BtConnectPermNotGranted : BluetoothState()
object BtDisabled : BluetoothState()
object Searching : BluetoothState()
data class PairedDeviceList(val deviceList: List<BluetoothDeviceStub>) : BluetoothState()