package com.peterlaurence.trekme.features.wifip2p.presentation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.wifip2p.presentation.ui.WifiP2pStateful

fun NavGraphBuilder.wifiP2p(onMainMenuClick: () -> Unit) {
    composable(wifiP2pDestination) {
        WifiP2pStateful(
            onMainMenuClick = onMainMenuClick
        )
    }
}

const val wifiP2pDestination = "wifiP2pDestination"