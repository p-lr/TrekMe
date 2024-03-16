package com.peterlaurence.trekme.features.gpspro.presentation.ui.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.peterlaurence.trekme.features.gpspro.presentation.ui.screens.BtDeviceSettingsStateful
import com.peterlaurence.trekme.features.gpspro.presentation.ui.screens.GpsProStateful
import com.peterlaurence.trekme.util.android.activity


fun NavGraphBuilder.gpsProGraph(
    navController: NavController,
    onMainMenuClick: () -> Unit
) {
    navigation(startDestination = gpsProMainDest, route = gpsProGraph) {
        gpsProMainDestination(
            onMainMenuClick = onMainMenuClick,
            onShowBtDeviceSettings = { navController.navigate(btDeviceSettingsDest) }
        )
        btDeviceSettingsDestination(
            onBack = navController::popBackStack
        )
    }
}

const val gpsProGraph = "gpsProGraph"
const val gpsProMainDest = "gpsProMainDestination"
private const val btDeviceSettingsDest = "btDeviceSettingsDestination"

private fun NavGraphBuilder.gpsProMainDestination(
    onMainMenuClick: () -> Unit,
    onShowBtDeviceSettings: () -> Unit,
) {
    composable(route = gpsProMainDest) {
        GpsProStateful(
            viewModel = hiltViewModel(viewModelStoreOwner = LocalContext.current.activity),
            onMainMenuClick = onMainMenuClick,
            onShowBtDeviceSettings = onShowBtDeviceSettings
        )
    }
}

private fun NavGraphBuilder.btDeviceSettingsDestination(
    onBack: () -> Unit
) {
    composable(route = btDeviceSettingsDest) {
        BtDeviceSettingsStateful(
            viewModel = hiltViewModel(viewModelStoreOwner = LocalContext.current.activity),
            onBackClick = onBack
        )
    }
}