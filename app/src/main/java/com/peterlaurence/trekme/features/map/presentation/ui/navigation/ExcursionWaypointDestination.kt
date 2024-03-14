package com.peterlaurence.trekme.features.map.presentation.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.peterlaurence.trekme.features.map.presentation.ui.screens.ExcursionWaypointEditStateful

private const val waypointEditDestination = "wpt_edit_dest"
private const val waypointArgId = "wpt_arg"
private const val excursionArgId = "excursion_arg"

fun NavGraphBuilder.excursionWaypointEditScreen(
    onBack: () -> Unit
) {
    composable(
        route = "$waypointEditDestination/{$waypointArgId}/{$excursionArgId}",
        arguments = listOf(
            navArgument(waypointArgId) { type = NavType.StringType },
            navArgument(excursionArgId) { type = NavType.StringType },
        )
    ) {
        ExcursionWaypointEditStateful(onBackAction = onBack)
    }
}

fun NavController.navigateToExcursionWaypointEdit(waypointId: String, excursionId: String) {
    navigate("$waypointEditDestination/$waypointId/$excursionId")
}

internal class ExcursionWaypointEditArgs(val waypointId: String, val excursionId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[waypointArgId]),
        checkNotNull(savedStateHandle[excursionArgId])
    )
}