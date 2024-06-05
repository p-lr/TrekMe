package com.peterlaurence.trekme.features.map.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.map.presentation.ui.screens.MarkersManageStateful

private const val markersDestination = "markers_dest"

fun NavGraphBuilder.markersManageScreen(
    onBackClick: () -> Unit
) {
    composable(markersDestination) {
        MarkersManageStateful(
            onBackClick = onBackClick
        )
    }
}

fun NavController.navigateToMarkersManage() {
    navigate(markersDestination)
}