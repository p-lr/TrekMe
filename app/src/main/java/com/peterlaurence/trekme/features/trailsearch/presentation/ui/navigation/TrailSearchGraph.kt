package com.peterlaurence.trekme.features.trailsearch.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation


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