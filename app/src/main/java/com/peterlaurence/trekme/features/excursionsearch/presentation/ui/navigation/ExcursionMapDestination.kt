package com.peterlaurence.trekme.features.excursionsearch.presentation.ui.navigation

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.excursionsearch.presentation.ui.screen.ExcursionMapStateful
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.ExcursionMapViewModel

private const val excursionMapDestination = "excursion_map"

fun NavGraphBuilder.excursionMapDestination(
    navController: NavController,
    onGoToMapList: () -> Unit
) {
    composable(excursionMapDestination) {
        val startGraphEntry = remember(it) {
            navController.getBackStackEntry(excursionSearchDestination)
        }
        val viewModel: ExcursionMapViewModel = hiltViewModel(startGraphEntry)
        ExcursionMapStateful(
            viewModel,
            onBack = { navController.navigateUp() },
            onGoToMapList = onGoToMapList
        )
    }
}

fun NavController.navigateToExcursionMap() {
    navigate(excursionMapDestination)
}