package com.peterlaurence.trekme.features.mapcreate.presentation.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.MapSourceListStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.gateway.IgnGatewayStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.overlay.LayerOverlayStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.screen.WmtsStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.MapSourceListViewModel


fun NavGraphBuilder.mapCreateGraph(
    navController: NavController,
    onMenuClick: () -> Unit,
    onNavigateToShop: () -> Unit,
) {
    navigation(startDestination = mapSourceListDestination, route = mapCreateGraph) {
        mapSourceListDestination(
            onMenuClick,
            onNavigateToWmtsScreen = { navController.navigate(wmtsDestination) },
            onNavigateToOfferGateway = { navController.navigate(ignGatewayDestination) }
        )
        ignGatewayDestination(
            onNavigateToWmtsScreen = {
                navController.navigate(wmtsDestination) {
                    popUpTo(mapSourceListDestination)
                }
            },
            onNavigateToShop = onNavigateToShop,
            onBack = { navController.popBackStack() }
        )
        wmtsDestination(
            onNavigateToOverlayLayers = { navController.navigateToOverlayLayers(it) },
            onNavigateToShop = onNavigateToShop,
            onBack = { navController.popBackStack() }
        )
        overlayLayersDestination(
            onBack = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.mapSourceListDestination(
    onMenuClick: () -> Unit,
    onNavigateToWmtsScreen: () -> Unit,
    onNavigateToOfferGateway: () -> Unit
) {
    composable(route = mapSourceListDestination) {
        val viewModel: MapSourceListViewModel = hiltViewModel()
        MapSourceListStateful(
            viewModel = viewModel,
            onSourceClick = {
                viewModel.setMapSource(it)
                if (it == WmtsSource.IGN) {
                    onNavigateToOfferGateway()
                } else {
                    onNavigateToWmtsScreen()
                }
            },
            onMainMenuClick = onMenuClick
        )
    }
}

private fun NavGraphBuilder.ignGatewayDestination(
    onNavigateToWmtsScreen: () -> Unit,
    onNavigateToShop: () -> Unit,
    onBack: () -> Unit
) {
    composable(route = ignGatewayDestination) {
        IgnGatewayStateful(
            viewModel = hiltViewModel(),
            onNavigateToWmtsScreen = onNavigateToWmtsScreen,
            onNavigateToShop = onNavigateToShop,
            onBack = onBack
        )
    }
}

private fun NavGraphBuilder.wmtsDestination(
    onNavigateToOverlayLayers: (WmtsSource) -> Unit,
    onNavigateToShop: () -> Unit,
    onBack: () -> Unit
) {
    composable(route = wmtsDestination) {
        WmtsStateful(
            viewModel = hiltViewModel(),
            onBoardingViewModel = hiltViewModel(),
            onShowLayerOverlay = onNavigateToOverlayLayers,
            onGoToShop = onNavigateToShop,
            onBack = onBack
        )
    }
}

private fun NavGraphBuilder.overlayLayersDestination(
    onBack: () -> Unit
) {
    composable(
        route = "$overlayLayersDestination/{$layerOverlayArg}",
        listOf(
            navArgument(layerOverlayArg) { type = NavType.StringType },
        )
    ) {
        LayerOverlayStateful(
            viewModel = hiltViewModel(),
            onBack = onBack
        )
    }
}

private fun NavController.navigateToOverlayLayers(wmtsSource: WmtsSource) {
    navigate("$overlayLayersDestination/$wmtsSource")
}

internal class LayerOverlayArg(val wmtsSource: WmtsSource) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        WmtsSource.valueOf(
            checkNotNull(
                savedStateHandle[layerOverlayArg]
            )
        )
    )
}

private const val layerOverlayArg = "layerOverlayArg"

const val mapCreateGraph = "mapcreateGraph"
const val mapSourceListDestination = "mapSourceListDestination"
const val wmtsDestination = "wmtsDestination"
private const val ignGatewayDestination = "ignGatewayDestination"
private const val overlayLayersDestination = "overlayLayersDestination"