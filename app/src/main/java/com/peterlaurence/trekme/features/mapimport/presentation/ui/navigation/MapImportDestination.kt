package com.peterlaurence.trekme.features.mapimport.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.mapimport.presentation.ui.screen.MapImportUiStateful

fun NavGraphBuilder.mapImport(
    onNavigateToMapList: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    composable(mapImportDestination) {
        MapImportUiStateful(
            onShowMapList = onNavigateToMapList,
            onMainMenuClick = onMainMenuClick
        )
    }
}

const val mapImportDestination = "mapImportDestination"