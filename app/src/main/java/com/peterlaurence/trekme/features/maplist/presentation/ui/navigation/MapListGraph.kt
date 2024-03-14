package com.peterlaurence.trekme.features.maplist.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import java.util.UUID


@Composable
fun MapListGraph(
    onNavigateToMapCreate: () -> Unit,
    onNavigateToMap: (UUID) -> Unit,
    onNavigateToExcursionSearch: () -> Unit,
    onNavigateToShop: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = mapListDestinationRoute) {
        mapListDestination(
            onNavigateToMapCreate = onNavigateToMapCreate,
            onNavigateToMapSettings = { navController.navigate(mapSettingsGraphRoute) },
            onNavigateToMap = onNavigateToMap,
            onNavigateToExcursionSearch = onNavigateToExcursionSearch,
            onMainMenuClick = onMainMenuClick
        )

        mapSettingsGraph(navController, onNavigateToShop)
    }
}


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