package com.peterlaurence.trekme.features.excursionsearch.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.excursionsearch.presentation.ui.screen.ExcursionSearchStateful

internal const val excursionSearchDestination = "excursion_search"

fun NavGraphBuilder.excursionSearchScreen(
    onNavigateToMap: () -> Unit,
    onMenuClick: () -> Unit
) {
    composable(excursionSearchDestination) {
        ExcursionSearchStateful(
            onMenuClick = onMenuClick,
            onNavigateToExcursionMap = onNavigateToMap
        )
    }
}
