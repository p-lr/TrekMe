package com.peterlaurence.trekme.main.eventhandler

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.util.android.requestBackgroundLocationPermission
import com.peterlaurence.trekme.util.android.requestNearbyWifiPermission
import com.peterlaurence.trekme.util.android.requestNotificationPermission
import kotlinx.coroutines.launch

/**
 * Handles application wide permission requests. This class is intended to be used by the main
 * activity only.
 */
class PermissionRequestHandler(
    private val activity: AppCompatActivity,
    private val lifecycle: Lifecycle,
    private val appEventBus: AppEventBus,
    private val gpsProEvents: GpsProEvents
) {

    private val bluetoothLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> appEventBus.bluetoothEnabled(true)
                Activity.RESULT_CANCELED -> appEventBus.bluetoothEnabled(false)
            }
        }

    private val requestBtConnectPermissionLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            gpsProEvents.postBluetoothPermissionResult(isGranted)
        }

    init {
        lifecycle.coroutineScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appEventBus.requestBackgroundLocationSignal.collect {
                    requestBackgroundLocationPermission(activity)
                }
            }
        }

        lifecycle.coroutineScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appEventBus.requestBluetoothEnableFlow.collect {
                    bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
            }
        }

        lifecycle.coroutineScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                gpsProEvents.requestBluetoothPermissionFlow.collect {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        requestBtConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                }
            }
        }

        lifecycle.coroutineScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appEventBus.requestNotificationPermFlow.collect {
                    requestNotificationPermission(activity)
                }
            }
        }

        lifecycle.coroutineScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appEventBus.requestNearbyWifiDevicesPermFlow.collect {
                    requestNearbyWifiPermission(activity)
                }
            }
        }
    }
}