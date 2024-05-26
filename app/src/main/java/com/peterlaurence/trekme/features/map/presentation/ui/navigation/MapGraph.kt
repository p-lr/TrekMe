package com.peterlaurence.trekme.features.map.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation


fun NavGraphBuilder.mapGraph(
    navController: NavController,
    onNavigateToShop: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    navigation(startDestination = mapDestination, route = mapSubGraph) {
        mapScreen(
            onNavigateToTrackManage = { navController.navigateToTracksManage() },
            onNavigateToMarkersManage = { navController.navigateToMarkersManage() },
            onNavigateToMarkerEdit = { markerId, mapId ->
                navController.navigateToMarkerEdit(markerId, mapId.toString())
            },
            onNavigateToExcursionWaypointEdit = { waypointId, excursionId ->
                navController.navigateToExcursionWaypointEdit(waypointId, excursionId)
            },
            onNavigateToBeaconEdit = { beaconId, mapId ->
                navController.navigateToBeaconEdit(beaconId, mapId.toString())
            },
            onNavigateToShop = onNavigateToShop,
            onMainMenuClick = onMainMenuClick
        )

        tracksManageScreen(
            onNavigateToMap = navController::navigateUp,
            onBackClick = navController::navigateUp
        )

        markersManageScreen(
            onNavigateToMap = navController::navigateUp,
            onBackClick = navController::navigateUp
        )

        markerEditScreen(onBack = navController::navigateUp)

        excursionWaypointEditScreen(onBack = navController::navigateUp)

        beaconEditScreen(onBack = navController::navigateUp)
    }
}

internal const val mapSubGraph = "map_sub_graph"