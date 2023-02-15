package com.peterlaurence.trekme.features.map.presentation.ui.modal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.map.app.service.BeaconService
import com.peterlaurence.trekme.features.map.presentation.viewmodel.BeaconServiceLauncherViewModel
import com.peterlaurence.trekme.util.compose.annotatedStringResource

/**
 * On Android, when a map has beacons, we:
 * - Ask for background location permission
 * - Start a foreground service to continuously check if we're in the vicinity of a beacon. The
 * service is started even if the permission isn't granted yet, as it can work when the permission
 * is granted after the service startup.
 */
@Composable
fun BeaconServiceLauncher(
    viewModel: BeaconServiceLauncherViewModel = hiltViewModel()
) {
    var isShowingBackgroundLocationRationale by rememberSaveable { mutableStateOf(false) }
    val context by rememberUpdatedState(LocalContext.current)

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(viewModel.backgroundLocationRequest) {
        viewModel.backgroundLocationRequest.collect {
            /* Start the service */
            val intent = Intent(context, BeaconService::class.java)
            context.startService(intent)

            if (Build.VERSION.SDK_INT < 29) return@collect

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                isShowingBackgroundLocationRationale = true
            }
        }
    }

    if (isShowingBackgroundLocationRationale) {
        val onDismiss = {
            isShowingBackgroundLocationRationale = false
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        AlertDialog(
            text = {
                Text(
                    text = annotatedStringResource(id = R.string.beacon_background_loc_perm),
                    fontSize = 17.sp
                )
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.ok_dialog))
                }
            }
        )
    }
}