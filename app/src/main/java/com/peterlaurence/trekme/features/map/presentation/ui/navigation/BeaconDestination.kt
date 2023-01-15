package com.peterlaurence.trekme.features.map.presentation.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.map.presentation.ui.screens.BeaconEditStateful

private const val beaconEditDestination = "beacon_edit_dest"
private const val beaconArgId = "beacon_arg"
private const val mapArgId = "map_arg"

fun NavGraphBuilder.beaconEditScreen(
    onBack: () -> Unit
) {
    composable(
        route = "$beaconEditDestination/{$beaconArgId}/{$mapArgId}",
        arguments = listOf(
            navArgument(beaconArgId) { type = NavType.StringType },
            navArgument(mapArgId) { type = NavType.StringType },
        )
    ) {
        TrekMeTheme {
            BeaconEditStateful(onBackAction = onBack)
        }
    }
}

fun NavController.navigateToBeaconEdit(beaconId: String, mapId: String) {
    navigate("$beaconEditDestination/$beaconId/$mapId")
}

internal class BeaconEditArgs(val beaconId: String, val mapId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[beaconArgId]), checkNotNull(savedStateHandle[mapArgId])
    )
}