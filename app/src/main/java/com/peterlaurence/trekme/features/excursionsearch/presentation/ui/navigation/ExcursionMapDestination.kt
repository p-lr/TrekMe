package com.peterlaurence.trekme.features.excursionsearch.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.excursionsearch.presentation.ui.screen.ExcursionMapStateful

private const val excursionMapDestination = "excursion_map"

fun NavGraphBuilder.excursionMapDestination(

) {
    composable(excursionMapDestination) {
        ExcursionMapStateful()
    }
}

fun NavController.navigateToExcursionMap() {
    navigate(excursionMapDestination)
}