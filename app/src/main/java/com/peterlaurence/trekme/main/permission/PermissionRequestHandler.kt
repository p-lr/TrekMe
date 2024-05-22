package com.peterlaurence.trekme.main.permission

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.WarningDialog
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.LocationRationale
import com.peterlaurence.trekme.util.android.MIN_PERMISSIONS_ANDROID_9_AND_BELOW
import com.peterlaurence.trekme.util.android.activity
import com.peterlaurence.trekme.util.android.hasPermissions
import com.peterlaurence.trekme.util.android.requestNearbyWifiPermission
import com.peterlaurence.trekme.util.android.requestNotificationPermission
import com.peterlaurence.trekme.util.android.shouldShowBackgroundLocPermRationale
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import com.peterlaurence.trekme.util.compose.LifeCycleObserver
import com.peterlaurence.trekme.util.compose.showSnackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun PermissionRequestHandler(
    appEventBus: AppEventBus,
    gpsProEvents: GpsProEvents,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val context = LocalContext.current
    val activity = context.activity
    val storagePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted: Map<String, @JvmSuppressWildcards Boolean> ->
        if (isGranted.values.any { !it }) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.storage_perm_denied),
                    isLong = true,
                    actionLabel = context.getString(R.string.ok_dialog)
                )

                if (result == SnackbarResult.ActionPerformed) {
                    openAppSettings(activity)
                }
            }
        }
    }

    var isShowingAndroid9AndBelowRationale by remember { mutableStateOf(false) }

    fun requestStorageAndLocationPermissions() {
        if (!hasPermissions(context, *MIN_PERMISSIONS_ANDROID_9_AND_BELOW)) {
            isShowingAndroid9AndBelowRationale = true
        }
    }

    if (isShowingAndroid9AndBelowRationale) {
        WarningDialog(
            title = stringResource(id = R.string.warning_title),
            contentText = stringResource(id = R.string.no_storage_perm),
            onConfirmPressed = { storagePermLauncher.launch(MIN_PERMISSIONS_ANDROID_9_AND_BELOW) },
            confirmButtonText = stringResource(id = R.string.ok_dialog),
            onDismissRequest = {
                storagePermLauncher.launch(MIN_PERMISSIONS_ANDROID_9_AND_BELOW)
            }
        )
    }

    var isShowingLocationRationale by remember { mutableStateOf(false) }

    if (isShowingLocationRationale) {
        WarningDialog(
            title = stringResource(id = R.string.warning_title),
            contentText = stringResource(id = R.string.no_location_perm),
            onConfirmPressed = {
                isShowingLocationRationale = false
                openAppSettings(activity)
            },
            confirmButtonText = stringResource(id = R.string.ok_dialog),
            onDismissRequest = {
                isShowingLocationRationale = false
            }
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            isShowingLocationRationale = true
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

    LifeCycleObserver(
        onStart = {
            requestMinimalPermission()
        }
    )

    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> appEventBus.bluetoothEnabled(true)
            Activity.RESULT_CANCELED -> appEventBus.bluetoothEnabled(false)
        }
    }

    LaunchedEffectWithLifecycle(appEventBus.requestBluetoothEnableFlow) {
        bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    val requestBtConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        gpsProEvents.postBluetoothPermissionResult(granted)
    }

    LaunchedEffectWithLifecycle(gpsProEvents.requestBluetoothPermissionFlow) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBtConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    var isShowingBackgroundLocationRationale by rememberSaveable {
        mutableStateOf<AppEventBus.BackgroundLocationRequest?>(
            null
        )
    }

    var backgroundLocationRequest: AppEventBus.BackgroundLocationRequest? by remember {
        mutableStateOf(
            null,
            policy = neverEqualPolicy()
        )
    }
    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            backgroundLocationRequest?.result?.send(granted)
        }
    }

    LaunchedEffectWithLifecycle(appEventBus.requestBackgroundLocationSignal) { request ->
        if (Build.VERSION.SDK_INT < 29) return@LaunchedEffectWithLifecycle
        backgroundLocationRequest = request
        if (shouldShowBackgroundLocPermRationale(activity)) {
            isShowingBackgroundLocationRationale = request
        } else {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    isShowingBackgroundLocationRationale?.also { request ->
        LocationRationale(
            text = context.getString(request.rationaleId),
            onConfirm = {
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                isShowingBackgroundLocationRationale = null
            },
            onIgnore = {
                scope.launch {
                    request.result.send(false)
                }
                isShowingBackgroundLocationRationale = null
            },
        )
    }

    LaunchedEffectWithLifecycle(appEventBus.requestNotificationPermFlow) {
        requestNotificationPermission(activity)
    }

    LaunchedEffectWithLifecycle(appEventBus.requestNearbyWifiDevicesPermFlow) {
        requestNearbyWifiPermission(activity)
    }
}

private fun openAppSettings(activity: Activity) {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri = Uri.fromParts("package", activity.packageName, null)
    intent.data = uri
    activity.startActivity(intent)
}