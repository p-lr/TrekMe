package com.peterlaurence.trekme.features.map.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController


@Composable
fun MapGraph(
    modifier: Modifier = Modifier,
    onNavigateToShop: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = mapDestination,
        modifier = modifier
    ) {
        mapScreen(
            onNavigateToTrackManage = { navController.navigateToTracksManage() },
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

        markerEditScreen(onBack = navController::navigateUp)

        excursionWaypointEditScreen(onBack = navController::navigateUp)

        beaconEditScreen(onBack = navController::navigateUp)
    }
}

fun NavGraphBuilder.mapGraph(
    navController: NavController,
    onNavigateToShop: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    navigation(startDestination = mapDestination, route = mapSubGraph) {
        mapScreen(
            onNavigateToTrackManage = { navController.navigateToTracksManage() },
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

        markerEditScreen(onBack = navController::navigateUp)

        excursionWaypointEditScreen(onBack = navController::navigateUp)

        beaconEditScreen(onBack = navController::navigateUp)
    }
}

internal const val mapSubGraph = "map_sub_graph"