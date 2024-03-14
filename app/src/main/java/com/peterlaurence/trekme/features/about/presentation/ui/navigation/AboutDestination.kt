package com.peterlaurence.trekme.features.about.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.about.presentation.ui.AboutStateful

fun NavGraphBuilder.about(onMainMenuClick: () -> Unit) {
    composable(aboutDestination) {
        AboutStateful(
            onMainMenuClick = onMainMenuClick
        )
    }
}

const val aboutDestination = "aboutDestination"