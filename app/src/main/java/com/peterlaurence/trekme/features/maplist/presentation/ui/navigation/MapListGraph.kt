package com.peterlaurence.trekme.features.maplist.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import java.util.UUID


fun NavGraphBuilder.mapListGraph(
    navController: NavController,
    onNavigateToMapCreate: () -> Unit,
    onNavigateToMap: (UUID) -> Unit,
    onNavigateToExcursionSearch: () -> Unit,
    onNavigateToShop: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    navigation(startDestination = mapListDestinationRoute, route = mapListSubGraph) {
        mapListDestination(
            onNavigateToMapCreate = onNavigateToMapCreate,
            onNavigateToMapSettings = {
                if (mapSettingsDestination == navController.currentDestination?.route) return@mapListDestination
                navController.navigate(mapSettingsGraphRoute)
            },
            onNavigateToMap = onNavigateToMap,
            onNavigateToExcursionSearch = onNavigateToExcursionSearch,
            onMainMenuClick = onMainMenuClick
        )

        mapSettingsGraph(navController, onNavigateToShop)
    }
}

internal const val mapListSubGraph = "maplist_sub_graph"