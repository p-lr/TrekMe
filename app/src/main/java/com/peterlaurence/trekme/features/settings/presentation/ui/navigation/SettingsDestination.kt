package com.peterlaurence.trekme.features.settings.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.settings.presentation.ui.SettingsStateful

fun NavGraphBuilder.settings(
    onBackClick: () -> Unit
) {
    composable(route = settingsDestination) {
        SettingsStateful(
            onBackClick = onBackClick
        )
    }
}

const val settingsDestination = "settingsDestination"