package com.peterlaurence.trekme.features.maplist.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.peterlaurence.trekme.features.maplist.presentation.ui.screens.CalibrationStateful
import com.peterlaurence.trekme.features.maplist.presentation.ui.screens.MapSettingsStateful

const val mapSettingsGraphRoute = "mapSettingsGraphRoute"
private const val mapSettingsDestination = "mapSettingsDestination"
private const val mapCalibrationDestination = "mapCalibrationDestination"

fun NavGraphBuilder.mapSettingsGraph(navController: NavController, onNavigateToShop: () -> Unit) {
    navigation(route = mapSettingsGraphRoute, startDestination = mapSettingsDestination) {
        composable(route = mapSettingsDestination) {
            MapSettingsStateful(
                onNavigateToCalibration = { navController.navigate(mapCalibrationDestination) },
                onNavigateToShop = onNavigateToShop,
                onBackClick = navController::navigateUp
            )
        }

        composable(route = mapCalibrationDestination) {
            CalibrationStateful(onBackClick = navController::navigateUp)
        }
    }
}