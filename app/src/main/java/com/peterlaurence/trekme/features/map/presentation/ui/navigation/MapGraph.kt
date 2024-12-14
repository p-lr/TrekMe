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
            onNavigateToTrackCreate = { navController.navigate(it) },
            onMainMenuClick = onMainMenuClick
        )

        tracksManageScreen(
            onNavigateToMap = navController::popBackStack,
            onBackClick = navController::popBackStack
        )

        markersManageScreen(
            onEditMarker = { markerId, mapId ->
                navController.navigateToMarkerEdit(markerId, mapId)
            },
            onEditWaypoint = { wptId, excursionId ->
                navController.navigateToExcursionWaypointEdit(wptId, excursionId)
            },
            onBackClick = navController::popBackStack
        )

        markerEditScreen(onBack = navController::popBackStack)

        excursionWaypointEditScreen(onBack = navController::popBackStack)

        beaconEditScreen(onBack = navController::popBackStack)

        trackCreateScreen(
            onNavigateToShop = onNavigateToShop,
            onBack = navController::popBackStack
        )
    }
}

internal const val mapSubGraph = "map_sub_graph"