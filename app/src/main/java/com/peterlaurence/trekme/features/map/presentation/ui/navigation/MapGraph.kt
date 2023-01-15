package com.peterlaurence.trekme.features.map.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController


@Composable
fun MapGraph(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = mapDestination,
        modifier = modifier
    ) {
        mapScreen(
            onNavigateToTrackManage = { navController.navigateToTracksManage() }
        )

        tracksManageScreen(
            onNavigateToRoute = { navController.navigateUp() },
            onMenuClick = onMenuClick
        )
    }
}