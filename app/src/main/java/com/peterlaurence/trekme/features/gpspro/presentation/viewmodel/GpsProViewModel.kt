package com.peterlaurence.trekme.features.gpspro.presentation.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.core.location.domain.model.InternalGps
import com.peterlaurence.trekme.core.location.domain.model.LocationProducerBtInfo
import com.peterlaurence.trekme.core.location.domain.model.LocationProducerInfo
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.features.gpspro.domain.repositories.GpsProDiagnosisRepo
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.features.gpspro.domain.repositories.DiagnosisAwaitingSave
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class GpsProViewModel @Inject constructor(
    private val settings: Settings,
    private val app: Application,
    private val appEventBus: AppEventBus,
    private val gpsProEvents: GpsProEvents,
    private val diagnosisRepo: GpsProDiagnosisRepo,
) : ViewModel() {
    var bluetoothState by mutableStateOf<BluetoothState>(Searching)
    var isHostSelected by mutableStateOf(false)
    var isDiagnosisRunning = diagnosisRepo.diagnosisRunningStateFlow

    private var diagnosisPending: String? = null
    private val _diagnosisEvent = Channel<String>(1)
    val diagnosisEvent = _diagnosisEvent.receiveAsFlow()

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
    @SuppressLint("MissingPermission")
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

    fun onRequestSaveDiagnosis() = viewModelScope.launch {
        val repoState = diagnosisRepo.diagnosisRunningStateFlow.value
        val diagnosis = if (repoState is DiagnosisAwaitingSave) {
            repoState.fileContent
        } else return@launch

        diagnosisPending = diagnosis
        _diagnosisEvent.send(diagnosis)

        diagnosisRepo.diagnosisSaved()
    }

    fun writeDiagnosisFileTo(uri: Uri) {
        val fileContent = diagnosisPending ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                app.applicationContext.contentResolver.openFileDescriptor(uri, "wt")?.use {
                    FileOutputStream(it.fileDescriptor).use { fos ->
                        fos.write(fileContent.toByteArray())
                    }
                }
                appEventBus.postMessage(StandardMessage(app.applicationContext.getString(R.string.bt_device_frgmt_record_done)))
            } catch (e: Exception) {
                appEventBus.postMessage(StandardMessage(app.applicationContext.getString(R.string.bt_device_frgmt_record_error)))
            }
        }
    }
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