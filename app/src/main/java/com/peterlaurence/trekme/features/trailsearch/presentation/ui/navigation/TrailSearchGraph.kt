package com.peterlaurence.trekme.features.trailsearch.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController


@Composable
fun TrailSearchGraph(
    modifier: Modifier = Modifier,
    onGoToMapList: () -> Unit,
    onGoToShop: () -> Unit,
    onGoToMapCreation: () -> Unit = {}
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = trailMapDestination,
        modifier = modifier
    ) {
        trailMapDestination(
            onGoToMapList = onGoToMapList,
            onGoToShop = onGoToShop,
            onGoToMapCreation = onGoToMapCreation
        )
    }
}

fun NavGraphBuilder.trailSearchGraph(
    onGoToMapList: () -> Unit,
    onGoToShop: () -> Unit,
    onGoToMapCreation: () -> Unit = {}
) {
    navigation(startDestination = trailMapDestination, route = trailSearchGraph) {
        trailMapDestination(
            onGoToMapList = onGoToMapList,
            onGoToShop = onGoToShop,
            onGoToMapCreation = onGoToMapCreation
        )
    }
}

const val trailSearchGraph = "trailSearchGraph"