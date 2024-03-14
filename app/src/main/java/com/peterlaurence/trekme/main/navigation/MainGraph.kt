package com.peterlaurence.trekme.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.peterlaurence.trekme.features.about.presentation.ui.navigation.about
import com.peterlaurence.trekme.features.about.presentation.ui.navigation.aboutDestination
import com.peterlaurence.trekme.features.gpspro.presentation.ui.navigation.gpsProGraph
import com.peterlaurence.trekme.features.gpspro.presentation.ui.navigation.gpsProMainDest
import com.peterlaurence.trekme.features.map.presentation.ui.navigation.mapDestination
import com.peterlaurence.trekme.features.map.presentation.ui.navigation.mapGraph
import com.peterlaurence.trekme.features.map.presentation.ui.navigation.mapSubGraph
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.navigation.mapCreateGraph
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.navigation.mapSourceListDestination
import com.peterlaurence.trekme.features.mapimport.presentation.ui.navigation.mapImport
import com.peterlaurence.trekme.features.mapimport.presentation.ui.navigation.mapImportDestination
import com.peterlaurence.trekme.features.maplist.presentation.ui.navigation.mapListDestinationRoute
import com.peterlaurence.trekme.features.maplist.presentation.ui.navigation.mapListGraph
import com.peterlaurence.trekme.features.maplist.presentation.ui.navigation.mapListSubGraph
import com.peterlaurence.trekme.features.record.presentation.ui.navigation.recordGraph
import com.peterlaurence.trekme.features.record.presentation.ui.navigation.recordListDestination
import com.peterlaurence.trekme.features.settings.presentation.ui.navigation.settings
import com.peterlaurence.trekme.features.settings.presentation.ui.navigation.settingsDestination
import com.peterlaurence.trekme.features.shop.presentation.ui.navigation.shop
import com.peterlaurence.trekme.features.shop.presentation.ui.navigation.shopDestination
import com.peterlaurence.trekme.features.trailsearch.presentation.ui.navigation.trailMapDestination
import com.peterlaurence.trekme.features.trailsearch.presentation.ui.navigation.trailSearchGraph
import com.peterlaurence.trekme.features.wifip2p.presentation.ui.navigation.wifiP2p
import com.peterlaurence.trekme.features.wifip2p.presentation.ui.navigation.wifiP2pDestination

@Composable
fun MainGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    onMainMenuClick: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = mapListSubGraph,
        modifier = modifier
    ) {
        mapListGraph(
            navController = navController,
            onNavigateToMapCreate = { navController.navigateToMapCreation() },
            onNavigateToMap = { id -> navController.navigateToMap() },
            onNavigateToExcursionSearch = { navController.navigateToTrailSearch() },
            onNavigateToShop = { navController.navigateToShop() },
            onMainMenuClick = onMainMenuClick
        )
        mapGraph(
            navController = navController,
            onNavigateToShop = { navController.navigateToShop() },
            onMainMenuClick = onMainMenuClick
        )
        mapCreateGraph(
            navController = navController,
            onMenuClick = onMainMenuClick,
            onNavigateToShop = { navController.navigateToShop() }
        )
        recordGraph(
            navController = navController,
            onMainMenuClick = onMainMenuClick,
            onNavigateToTrailSearch = { navController.navigateToTrailSearch() },
        )
        trailSearchGraph(
            onGoToMapList = { navController.navigateToMapList() },
            onGoToShop = { navController.navigateToShop() },
            onGoToMapCreation = { navController.navigateToMapCreation() }
        )
        gpsProGraph(
            navController = navController,
            onMainMenuClick = onMainMenuClick
        )
        mapImport(
            onNavigateToMapList = { navController.navigateToMapList() },
            onMainMenuClick = onMainMenuClick
        )
        wifiP2p(
            onMainMenuClick = onMainMenuClick
        )
        settings(
            onMainMenuClick = onMainMenuClick
        )
        shop(
            onMainMenuClick = onMainMenuClick
        )
        about(
            onMainMenuClick = onMainMenuClick
        )
    }
}

fun NavController.navigateToMapList() {
    if (mapListDestinationRoute == currentDestination?.route) return
    navigate(mapListSubGraph) {
        popUpTo(mapListSubGraph)
    }
}

fun NavController.navigateToMap() {
    if (mapDestination == currentDestination?.route) return
    navigate(mapSubGraph)
}

fun NavController.navigateToMapCreation() {
    if (mapSourceListDestination == currentDestination?.route) return
    navigate(mapCreateGraph) {
        popUpTo(mapCreateGraph)
    }
}

fun NavController.navigateToRecord() {
    if (recordListDestination == currentDestination?.route) return
    navigate(recordGraph) {
        popUpTo(recordGraph)
    }
}

fun NavController.navigateToTrailSearch() {
    if (trailMapDestination == currentDestination?.route) return
    navigate(trailSearchGraph) {
        popUpTo(trailSearchGraph)
    }
}

fun NavController.navigateToGpsPro() {
    if (gpsProMainDest == currentDestination?.route) return
    navigate(gpsProGraph) {
        popUpTo(gpsProGraph)
    }
}

fun NavController.navigateToMapImport() {
    if (mapImportDestination == currentDestination?.route) return
    navigate(mapImportDestination) {
        popUpTo(mapImportDestination)
    }
}

fun NavController.navigateToWifiP2p() {
    if (wifiP2pDestination == currentDestination?.route) return
    navigate(wifiP2pDestination) {
        popUpTo(wifiP2pDestination)
    }
}

fun NavController.navigateToSettings() {
    if (settingsDestination == currentDestination?.route) return
    navigate(settingsDestination)
}

fun NavController.navigateToShop() {
    if (shopDestination == currentDestination?.route) return
    navigate(shopDestination)
}

fun NavController.navigateToAbout() {
    if (aboutDestination == currentDestination?.route) return
    navigate(aboutDestination)
}
