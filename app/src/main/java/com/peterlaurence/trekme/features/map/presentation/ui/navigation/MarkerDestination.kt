package com.peterlaurence.trekme.features.map.presentation.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.map.presentation.ui.screens.MarkerEditStateful

private const val markerEditDestination = "marker_edit_dest"
private const val markerArgId = "marker_arg"
private const val mapArgId = "map_arg"

fun NavGraphBuilder.markerEditScreen(
    onBack : () -> Unit
) {
    composable(
        "$markerEditDestination/{$markerArgId}/{$mapArgId}",
        arguments = listOf(
            navArgument(markerArgId) { type = NavType.StringType },
            navArgument(mapArgId) { type = NavType.StringType },
        )
    ) {
        TrekMeTheme {
            MarkerEditStateful(
                onBackAction = onBack
            )
        }
    }
}

fun NavController.navigateToMarkerEdit(markerId: String, mapId: String) {
    navigate("$markerEditDestination/$markerId/$mapId")
}

internal class MarkerEditArgs(val markerId: String, val mapId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[markerArgId]), checkNotNull(savedStateHandle[mapArgId])
    )
}