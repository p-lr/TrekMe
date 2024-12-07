package com.peterlaurence.trekme.features.map.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.map.presentation.ui.trackcreate.TrackCreateStateful
import kotlinx.serialization.Serializable


fun NavGraphBuilder.trackCreateScreen(
    onBack: () -> Unit
) {
    composable<TrackCreateScreenArgs> {
        TrackCreateStateful(onBack = onBack)
    }
}

@Serializable
data class TrackCreateScreenArgs(
    val mapId: String,
    val centroidX: Double,
    val centroidY: Double,
    val scale: Float,
    val excursionId: String?
)