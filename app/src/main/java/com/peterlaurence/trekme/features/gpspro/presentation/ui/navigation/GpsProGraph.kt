package com.peterlaurence.trekme.features.gpspro.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.peterlaurence.trekme.features.gpspro.presentation.ui.screens.BtDeviceSettingsStateful
import com.peterlaurence.trekme.features.gpspro.presentation.ui.screens.GpsProStateful
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.GpsProViewModel
import com.peterlaurence.trekme.util.android.activity

@Composable
fun GpsProGraph(
    onMainMenuClick: () -> Unit
) {
    val navController = rememberNavController()
    val viewModel: GpsProViewModel = hiltViewModel(viewModelStoreOwner = LocalContext.current.activity)

    NavHost(navController, startDestination = gpsProMainDest) {
        gpsProMainDestination(
            viewModel = viewModel,
            onMainMenuClick = onMainMenuClick,
            onShowBtDeviceSettings = { navController.navigate(btDeviceSettingsDest) }
        )
        btDeviceSettingsDestination(
            viewModel = viewModel,
            onBack = navController::popBackStack
        )
    }
}

private const val gpsProMainDest = "gpsProMainDestination"
private const val btDeviceSettingsDest = "btDeviceSettingsDestination"

private fun NavGraphBuilder.gpsProMainDestination(
    viewModel: GpsProViewModel,
    onMainMenuClick: () -> Unit,
    onShowBtDeviceSettings: () -> Unit,
) {
    composable(route = gpsProMainDest) {
        GpsProStateful(
            viewModel = viewModel,
            onMainMenuClick = onMainMenuClick,
            onShowBtDeviceSettings = onShowBtDeviceSettings
        )
    }
}

private fun NavGraphBuilder.btDeviceSettingsDestination(
    viewModel: GpsProViewModel,
    onBack: () -> Unit
) {
    composable(route = btDeviceSettingsDest) {
        BtDeviceSettingsStateful(
            viewModel = viewModel,
            onBackClick = onBack
        )
    }
}