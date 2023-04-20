package com.peterlaurence.trekme.features.excursionsearch.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController


@Composable
fun ExcursionSearchGraph(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = excursionSearchDestination,
        modifier = modifier
    ) {
        excursionSearchScreen(
            onNavigateToMap = { navController.navigateToExcursionMap() },
            onMenuClick = onMenuClick
        )
        excursionMapDestination()
    }
}