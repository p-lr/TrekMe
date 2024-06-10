package com.peterlaurence.trekme.features.map.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.map.presentation.ui.screens.MarkersManageStateful

private const val markersDestination = "markers_dest"

fun NavGraphBuilder.markersManageScreen(
    onEditMarker: (markerId: String, mapId: String) -> Unit,
    onEditWaypoint: (waypointId: String, excursionId: String) -> Unit,
    onBackClick: () -> Unit
) {
    composable(markersDestination) {
        MarkersManageStateful(
            onEditMarker = onEditMarker,
            onEditWaypoint = onEditWaypoint,
            onBackClick = onBackClick
        )
    }
}

fun NavController.navigateToMarkersManage() {
    navigate(markersDestination)
}