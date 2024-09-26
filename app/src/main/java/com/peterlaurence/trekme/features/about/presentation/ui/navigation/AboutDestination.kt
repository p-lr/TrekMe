package com.peterlaurence.trekme.features.about.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.about.presentation.ui.AboutStateful

fun NavGraphBuilder.about(onBackClick: () -> Unit) {
    composable(aboutDestination) {
        AboutStateful(
            onBackClick = onBackClick
        )
    }
}

const val aboutDestination = "aboutDestination"