package com.peterlaurence.trekme.features.maplist.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import java.util.UUID


@Composable
fun MapListGraph(
    onNavigateToMapCreate: () -> Unit,
    onNavigateToMap: (UUID) -> Unit,
    onNavigateToExcursionSearch: () -> Unit,
    onNavigateToShop: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = mapListDestinationRoute) {
        mapListDestination(
            onNavigateToMapCreate = onNavigateToMapCreate,
            onNavigateToMapSettings = { navController.navigate(mapSettingsGraphRoute) },
            onNavigateToMap = onNavigateToMap,
            onNavigateToExcursionSearch = onNavigateToExcursionSearch
        )

        mapSettingsGraph(navController, onNavigateToShop)
    }
}