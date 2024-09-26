package com.peterlaurence.trekme.features.shop.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.shop.presentation.ui.ShopStateful

fun NavGraphBuilder.shop(onBackClick: () -> Unit) {
    composable(shopDestination) {
        ShopStateful(onBackClick = onBackClick)
    }
}

const val shopDestination = "shopDestination"