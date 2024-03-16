package com.peterlaurence.trekme.features.map.presentation.ui.modal

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.features.map.app.service.BeaconService
import com.peterlaurence.trekme.features.map.presentation.viewmodel.BeaconServiceLauncherViewModel
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle

/**
 * On Android, when a map has beacons, we:
 * - Ask for background location permission
 * - Start a foreground service to continuously check if we're in the vicinity of a beacon. The
 * service is started only when the background location permission is granted.
 */
@Composable
fun BeaconServiceLauncher(
    viewModel: BeaconServiceLauncherViewModel = hiltViewModel()
) {
    val context by rememberUpdatedState(LocalContext.current)

    LaunchedEffectWithLifecycle(viewModel.startServiceEvent) {
        /* Start the service */
        val intent = Intent(context, BeaconService::class.java)
        context.startService(intent)
    }
}