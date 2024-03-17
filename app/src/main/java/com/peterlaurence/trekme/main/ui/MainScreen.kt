package com.peterlaurence.trekme.main.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.events.maparchive.MapArchiveEvents
import com.peterlaurence.trekme.main.ui.component.DrawerHeader
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.WarningDialog
import com.peterlaurence.trekme.main.eventhandler.BillingEventHandler
import com.peterlaurence.trekme.main.viewmodel.MainActivityEvent
import com.peterlaurence.trekme.main.viewmodel.MainActivityViewModel
import com.peterlaurence.trekme.main.eventhandler.HandleGenericMessages
import com.peterlaurence.trekme.main.eventhandler.MapArchiveEventHandler
import com.peterlaurence.trekme.main.eventhandler.MapDownloadEventHandler
import com.peterlaurence.trekme.main.eventhandler.RecordingEventHandler
import com.peterlaurence.trekme.main.ui.navigation.MainGraph
import com.peterlaurence.trekme.main.ui.navigation.navigateToAbout
import com.peterlaurence.trekme.main.ui.navigation.navigateToGpsPro
import com.peterlaurence.trekme.main.ui.navigation.navigateToMap
import com.peterlaurence.trekme.main.ui.navigation.navigateToMapCreation
import com.peterlaurence.trekme.main.ui.navigation.navigateToMapImport
import com.peterlaurence.trekme.main.ui.navigation.navigateToMapList
import com.peterlaurence.trekme.main.ui.navigation.navigateToRecord
import com.peterlaurence.trekme.main.ui.navigation.navigateToSettings
import com.peterlaurence.trekme.main.ui.navigation.navigateToShop
import com.peterlaurence.trekme.main.ui.navigation.navigateToTrailSearch
import com.peterlaurence.trekme.main.ui.navigation.navigateToWifiP2p
import com.peterlaurence.trekme.main.permission.PermissionRequestHandler
import com.peterlaurence.trekme.main.ui.component.HandleBackGesture
import com.peterlaurence.trekme.main.ui.component.MainActivityLifecycleObserver
import com.peterlaurence.trekme.main.viewmodel.RecordingEventHandlerViewModel
import com.peterlaurence.trekme.util.checkInternet
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun MainStateful(
    viewModel: MainActivityViewModel,
    recordingEventHandlerViewModel: RecordingEventHandlerViewModel,
    appEventBus: AppEventBus,
    gpsProEvents: GpsProEvents,
    mapArchiveEvents: MapArchiveEvents
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val context = LocalContext.current

    LaunchedEffectWithLifecycle(viewModel.eventFlow) { event ->
        when (event) {
            MainActivityEvent.ShowMap -> navController.navigateToMap()
            MainActivityEvent.ShowMapList -> navController.navigateToMapList()
            MainActivityEvent.ShowRecordings -> navController.navigateToRecord()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    var isShowingWarningDialog by remember { mutableStateOf<WarningMessage?>(null) }
    isShowingWarningDialog?.also {
        WarningDialog(
            title = it.title,
            contentText = it.msg,
            onDismissRequest = { isShowingWarningDialog = null }
        )
    }

    HandleBackGesture(drawerState, scope, navController, snackbarHostState)

    PermissionRequestHandler(
        appEventBus = appEventBus,
        gpsProEvents = gpsProEvents,
        snackbarHostState = snackbarHostState,
        scope = scope
    )

    RecordingEventHandler(
        recordingEventHandlerViewModel.gpxRecordEvents,
        onNewExcursion = recordingEventHandlerViewModel::onNewExcursionEvent
    )

    MapDownloadEventHandler(
        downloadEvents = viewModel.downloadEvents,
        navController = navController,
        snackbarHostState = snackbarHostState,
        scope = scope,
        context = context,
        onGoToMap = { uuid -> viewModel.onGoToMap(uuid) },
        onShowWarningDialog = { isShowingWarningDialog = it }
    )

    HandleGenericMessages(
        genericMessages = appEventBus.genericMessageEvents,
        scope = scope,
        snackbarHostState = snackbarHostState,
        onShowWarningDialog = { isShowingWarningDialog = it }
    )

    MapArchiveEventHandler(appEventBus, mapArchiveEvents)

    BillingEventHandler(appEventBus)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission should always be granted
    }

    val checkInternetPermission = {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.INTERNET
            ) == PackageManager.PERMISSION_DENIED
        ) {
            launcher.launch(Manifest.permission.INTERNET)
        }
    }

    val noInternet = stringResource(id = R.string.no_internet)
    // TODO: do this in each individual destination and not at this level
    val warnIfNotInternet = {
        scope.launch {
            if (!checkInternet()) {
                snackbarHostState.showSnackbar(noInternet)
            }
        }
    }

    val gpsProPurchased by viewModel.gpsProPurchased.collectAsState()
    val menuItems by remember {
        derivedStateOf {
            if (gpsProPurchased) MenuItem.entries else MenuItem.entries.filter { it != MenuItem.GpsPro }
        }
    }
    val selectedItem = remember { mutableStateOf(menuItems[0]) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                DrawerHeader()

                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painterResource(id = getIconForMenu(item)),
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(id = getNameForMenu(item))) },
                        selected = item == selectedItem.value,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                            }
                            selectedItem.value = item

                            when (item) {
                                MenuItem.MapList -> navController.navigateToMapList()
                                MenuItem.MapCreate -> {
                                    checkInternetPermission()
                                    warnIfNotInternet()
                                    navController.navigateToMapCreation()
                                }

                                MenuItem.Record -> navController.navigateToRecord()
                                MenuItem.TrailSearch -> {
                                    warnIfNotInternet()
                                    navController.navigateToTrailSearch()
                                }

                                MenuItem.GpsPro -> navController.navigateToGpsPro()
                                MenuItem.MapImport -> navController.navigateToMapImport()
                                MenuItem.WifiP2p -> navController.navigateToWifiP2p()
                                MenuItem.Settings -> navController.navigateToSettings()
                                MenuItem.Shop -> navController.navigateToShop()
                                MenuItem.About -> navController.navigateToAbout()
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        },
        content = {
            Box {
                MainGraph(
                    navController = navController,
                    onMainMenuClick = { scope.launch { drawerState.open() } }
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    )

    MainActivityLifecycleObserver(viewModel)
}

private fun getNameForMenu(menuItem: MenuItem): Int {
    return when (menuItem) {
        MenuItem.MapList -> R.string.select_map_menu_title
        MenuItem.MapCreate -> R.string.create_menu_title
        MenuItem.Record -> R.string.trails_menu_title
        MenuItem.TrailSearch -> R.string.trail_search_feature_menu
        MenuItem.GpsPro -> R.string.gps_plus_menu_title
        MenuItem.MapImport -> R.string.import_menu_title
        MenuItem.WifiP2p -> R.string.share_menu_title
        MenuItem.Settings -> R.string.settings_menu_title
        MenuItem.Shop -> R.string.shop_menu_title
        MenuItem.About -> R.string.about
    }
}

private fun getIconForMenu(menuItem: MenuItem): Int {
    return when (menuItem) {
        MenuItem.MapList -> R.drawable.ic_menu_gallery
        MenuItem.MapCreate -> R.drawable.ic_terrain_black_24dp
        MenuItem.Record -> R.drawable.folder
        MenuItem.TrailSearch -> R.drawable.ic_baseline_search_24
        MenuItem.GpsPro -> R.drawable.satellite_variant
        MenuItem.MapImport -> R.drawable.import_24dp
        MenuItem.WifiP2p -> R.drawable.ic_share_black_24dp
        MenuItem.Settings -> R.drawable.ic_settings_black_24dp
        MenuItem.Shop -> R.drawable.basket
        MenuItem.About -> R.drawable.help
    }
}

private enum class MenuItem {
    MapList, MapCreate, Record, TrailSearch, GpsPro, MapImport, WifiP2p, Settings, Shop, About
}