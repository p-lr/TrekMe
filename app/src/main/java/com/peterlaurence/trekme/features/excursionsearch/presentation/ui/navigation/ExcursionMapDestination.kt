package com.peterlaurence.trekme.features.excursionsearch.presentation.ui.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val excursionMapDestination = "excursion_map"

fun NavGraphBuilder.excursionMapDestination(

) {
    composable(excursionMapDestination) {
        Text(text = "hello")
    }
}

fun NavController.navigateToExcursionMap() {
    navigate(excursionMapDestination)
}