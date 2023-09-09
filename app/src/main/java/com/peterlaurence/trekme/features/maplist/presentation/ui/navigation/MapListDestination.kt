package com.peterlaurence.trekme.features.maplist.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.maplist.presentation.ui.screens.MapListStateful
import java.util.UUID

const val mapListDestinationRoute = "mapListDestination"

fun NavGraphBuilder.mapListDestination(
    onNavigateToMapCreate: () -> Unit,
    onNavigateToMapSettings: () -> Unit,
    onNavigateToMap: (UUID) -> Unit,
    onNavigateToExcursionSearch: () -> Unit,
) {
    composable(route = mapListDestinationRoute) {
        MapListStateful(
            onNavigateToMapCreate = onNavigateToMapCreate,
            onNavigateToMapSettings = onNavigateToMapSettings,
            onNavigateToMap = onNavigateToMap,
            onNavigateToExcursionSearch = onNavigateToExcursionSearch
        )
    }
}