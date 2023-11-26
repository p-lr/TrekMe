package com.peterlaurence.trekme.features.trailsearch.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.trailsearch.presentation.ui.screen.TrailMapStateful

internal const val trailMapDestination = "trail_map"

fun NavGraphBuilder.trailMapDestination(
    onGoToMapList: () -> Unit,
    onGoToShop: () -> Unit,
    onGoToMapCreation: () -> Unit
) {
    composable(trailMapDestination) {
        TrailMapStateful(
            onGoToMapList = onGoToMapList,
            onGoToShop = onGoToShop,
            onGoToMapCreation = onGoToMapCreation
        )
    }
}
