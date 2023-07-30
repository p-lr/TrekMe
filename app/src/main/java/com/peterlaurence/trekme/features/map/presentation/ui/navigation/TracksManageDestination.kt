package com.peterlaurence.trekme.features.map.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.map.presentation.ui.screens.TracksManageStateful

private const val tracksDestination = "tracks_dest"

fun NavGraphBuilder.tracksManageScreen(
    onNavigateToMap: () -> Unit,
    onBackClick: () -> Unit
) {
    composable(tracksDestination) {
        TrekMeTheme {
            TracksManageStateful(
                onNavigateToMap = onNavigateToMap,
                onBackClick = onBackClick
            )
        }
    }
}

fun NavController.navigateToTracksManage() {
    navigate(tracksDestination)
}