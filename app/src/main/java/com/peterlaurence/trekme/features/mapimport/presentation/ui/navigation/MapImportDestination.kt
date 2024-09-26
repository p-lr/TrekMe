package com.peterlaurence.trekme.features.mapimport.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.mapimport.presentation.ui.screen.MapImportUiStateful

fun NavGraphBuilder.mapImport(
    onNavigateToMapList: () -> Unit,
    onBackClick: () -> Unit
) {
    composable(mapImportDestination) {
        MapImportUiStateful(
            onShowMapList = onNavigateToMapList,
            onBackClick = onBackClick
        )
    }
}

const val mapImportDestination = "mapImportDestination"