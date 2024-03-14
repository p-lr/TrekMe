package com.peterlaurence.trekme.main.eventhandler

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.main.MainActivity
import com.peterlaurence.trekme.util.android.*
import kotlinx.coroutines.launch

/**
 * Handles application wide permission requests. This class is intended to be used by the main
 * activity only.
 */
class PermissionRequestHandler(
    private val activity: MainActivity,
    private val lifecycle: Lifecycle,
    private val appEventBus: AppEventBus,
    private val gpsProEvents: GpsProEvents
) {

    private val locationPermissionLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            // TODO: warn the user that a critical perm isn't granted
        }

    private val requestMapCreationLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            // Internet permission should always be granted
        }

    private val storagePermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantedMap ->
        if (grantedMap.values.any { !it }) {
            /* User has denied one of the critical permissions so we suggest navigating to the app settings */
            with(activity) {
                // TODO
//                Snackbar.make(
//                    binding.root,
//                    getString(R.string.storage_perm_denied),
//                    Snackbar.LENGTH_INDEFINITE
//                ).setAction(getString(R.string.ok_dialog)) {
//                    val intent = Intent()
//                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                    val uri = Uri.fromParts("package", activity.packageName, null)
//                    intent.data = uri
//                    startActivity(intent)
//                }.show()
            }
        }
    }

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

    fun requestMapCreationPermission() {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestMapCreationLauncher.launch(Manifest.permission.INTERNET)
        }
    }

    /**
     * Checks whether the app has permission to access fine location and (for Android < 10) to
     * write to device storage.
     * If the app does not have the requested permissions then the user will be prompted.
     */
    fun requestMinimalPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            /* We absolutely need storage and location perm under Android 10 */
            requestStorageAndLocationPermissions()
        } else {
            /* On Android 10 and above, we just need the location perm */
            val hasLocationPermission = ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasLocationPermission) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun requestStorageAndLocationPermissions() = with(activity) {
        if (!hasPermissions(this, *MIN_PERMISSIONS_ANDROID_9_AND_BELOW)) {
            showWarningDialog(
                getString(R.string.no_storage_perm),
                getString(R.string.warning_title),
                onDismiss = {
                    storagePermissionLauncher.launch(MIN_PERMISSIONS_ANDROID_9_AND_BELOW)
                },
            )
        }
    }
}