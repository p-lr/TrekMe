package com.peterlaurence.trekme.features.settings.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.settings.presentation.ui.SettingsStateful

fun NavGraphBuilder.settings(
    onMainMenuClick: () -> Unit
) {
    composable(route = settingsDestination) {
        SettingsStateful(
            onMainMenuClick = onMainMenuClick
        )
    }
}

const val settingsDestination = "settingsDestination"