package com.peterlaurence.trekme.features.mapcreate.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.MapSourceListStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.offergateway.ExtendedOfferGatewayStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.overlay.LayerOverlayStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.screen.WmtsStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.MapSourceListViewModel


@Composable
fun MapCreateGraph(
    onMenuClick: () -> Unit,
    onNavigateToShop: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = mapSourceListDestination) {
        mapSourceListDestination(
            onMenuClick,
            onNavigateToWmtsScreen = { navController.navigate(wmtsDestination) },
            onNavigateToOfferGateway = { navController.navigate(gatewayDestination) }
        )
        gatewayDestination(
            onNavigateToWmtsScreen = { navController.navigate(wmtsDestination) },
            onNavigateToShop = onNavigateToShop,
            onBack = { navController.popBackStack() }
        )
        wmtsDestination(
            onMenuClick,
            onNavigateToOverlayLayers = { navController.navigateToOverlayLayers(it) },
            onNavigateToShop = onNavigateToShop,
        )
        overlayLayersDestination(
            onBack = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.mapCreateGraph(
    navController: NavController,
    onMenuClick: () -> Unit,
    onNavigateToShop: () -> Unit,
) {
    navigation(startDestination = mapSourceListDestination, route = mapCreateGraph) {
        mapSourceListDestination(
            onMenuClick,
            onNavigateToWmtsScreen = { navController.navigate(wmtsDestination) },
            onNavigateToOfferGateway = { navController.navigate(gatewayDestination) }
        )
        gatewayDestination(
            onNavigateToWmtsScreen = {
                navController.navigate(wmtsDestination) {
                    popUpTo(mapSourceListDestination)
                }
            },
            onNavigateToShop = onNavigateToShop,
            onBack = { navController.popBackStack() }
        )
        wmtsDestination(
            onMenuClick,
            onNavigateToOverlayLayers = { navController.navigateToOverlayLayers(it) },
            onNavigateToShop = onNavigateToShop,
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

private fun NavGraphBuilder.gatewayDestination(
    onNavigateToWmtsScreen: () -> Unit,
    onNavigateToShop: () -> Unit,
    onBack: () -> Unit
) {
    composable(route = gatewayDestination) {
        ExtendedOfferGatewayStateful(
            viewModel = hiltViewModel(),
            onNavigateToWmtsScreen = onNavigateToWmtsScreen,
            onNavigateToShop = onNavigateToShop,
            onBack = onBack
        )
    }
}

private fun NavGraphBuilder.wmtsDestination(
    onMenuClick: () -> Unit,
    onNavigateToOverlayLayers: (WmtsSource) -> Unit,
    onNavigateToShop: () -> Unit,
) {
    composable(route = wmtsDestination) {
        WmtsStateful(
            viewModel = hiltViewModel(),
            onBoardingViewModel = hiltViewModel(),
            onShowLayerOverlay = onNavigateToOverlayLayers,
            onMenuClick = onMenuClick,
            onGoToShop = onNavigateToShop
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
private const val gatewayDestination = "extendedOfferGatewayDestination"
private const val overlayLayersDestination = "overlayLayersDestination"