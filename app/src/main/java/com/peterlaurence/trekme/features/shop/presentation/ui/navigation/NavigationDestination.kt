package com.peterlaurence.trekme.features.shop.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.shop.presentation.ui.ShopStateful

fun NavGraphBuilder.shop(onMainMenuClick: () -> Unit) {
    composable(shopDestination) {
        ShopStateful(onMainMenuClick = onMainMenuClick)
    }
}

const val shopDestination = "shopDestination"