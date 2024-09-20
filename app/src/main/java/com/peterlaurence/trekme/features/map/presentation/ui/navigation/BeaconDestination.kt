package com.peterlaurence.trekme.features.map.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.map.presentation.ui.screens.BeaconEditStateful
import kotlinx.serialization.Serializable

fun NavGraphBuilder.beaconEditScreen(
    onBack: () -> Unit
) {
    composable<BeaconEditScreenArgs> {
        BeaconEditStateful(onBackAction = onBack)
    }
}

fun NavController.navigateToBeaconEdit(beaconId: String, mapId: String) {
    navigate(BeaconEditScreenArgs(beaconId, mapId))
}

@Serializable
internal data class BeaconEditScreenArgs(val beaconId: String, val mapId: String)