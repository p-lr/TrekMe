package com.peterlaurence.trekme.features.excursionsearch.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.excursionsearch.presentation.ui.screen.ExcursionMapStateful

internal const val trailMapDestination = "trail_map"

fun NavGraphBuilder.trailMapDestination(
    navController: NavController,
    onGoToMapList: () -> Unit,
    onGoToShop: () -> Unit,
    onGoToMapCreation: () -> Unit
) {
    composable(trailMapDestination) {
        ExcursionMapStateful(
            onBack = { navController.navigateUp() },
            onGoToMapList = onGoToMapList,
            onGoToShop = onGoToShop,
            onGoToMapCreation = onGoToMapCreation
        )
    }
}
