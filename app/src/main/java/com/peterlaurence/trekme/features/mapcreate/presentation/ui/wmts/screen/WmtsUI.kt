package com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.screen

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.core.wmts.domain.model.Layer
import com.peterlaurence.trekme.core.wmts.domain.model.OsmAndHd
import com.peterlaurence.trekme.core.wmts.domain.model.OsmLayer
import com.peterlaurence.trekme.core.wmts.domain.model.Outdoors
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.features.common.presentation.ui.screens.ErrorScreen
import com.peterlaurence.trekme.features.common.presentation.ui.screens.LoadingScreen
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.DialogShape
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.OnBoardingTip
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.PopupOrigin
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.dialogs.AdvertTrekmeExtendedDialog
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.dialogs.LevelsDialogStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.dialogs.PrimaryLayerDialogStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components.GeoPlaceListUI
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components.WmtsAppBar
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.DownloadFormData
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.PrimaryLayerSelectionData
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.AreaSelection
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.Collapsed
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.GeoplaceList
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.Loading
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.MapReady
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.OnBoardingState
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.ShowTip
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.TopBarState
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.UiState
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.Wmts
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.WmtsError
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.WmtsEvent
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.WmtsOnBoardingViewModel
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.WmtsState
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.WmtsViewModel
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.DefaultCanvas
import ovh.plrapps.mapcompose.api.fullSize
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.abs
import kotlin.math.min

/**
 * Displays Google Maps - compatible tile matrix sets.
 * For example :
 *
 * [IGN WMTS](https://geoservices.ign.fr/documentation/geoservices/wmts.html). A `GetCapabilities`
 * request reveals that each level is square area. Here is an example for level 18 :
 * ```
 * <TileMatrix>
 *   <ows:Identifier>18</ows:Identifier>
 *   <ScaleDenominator>2132.7295838497840572</ScaleDenominator>
 *   <TopLeftCorner>
 *     -20037508.3427892476320267 20037508.3427892476320267
 *   </TopLeftCorner>
 *   <TileWidth>256</TileWidth>
 *   <TileHeight>256</TileHeight>
 *   <MatrixWidth>262144</MatrixWidth>
 *   <MatrixHeight>262144</MatrixHeight>
 * </TileMatrix>
 * ```
 * This level correspond to a 256 * 262144 = 67108864 px wide and height area.
 * The `TopLeftCorner` corner contains the WebMercator coordinates. The bottom right corner has
 * implicitly the opposite coordinates.
 * **Beware** that this "level 18" is actually the 19th level (matrix set starts at 0).
 *
 * The same settings can be seen at [USGS WMTS](https://basemap.nationalmap.gov/arcgis/rest/services/USGSTopo/MapServer/WMTS/1.0.0/WMTSCapabilities.xml)
 * for the "GoogleMapsCompatible" TileMatrixSet (and not the "default028mm" one).
 *
 * @since 11/05/2018
 */
@Composable
fun WmtsStateful(
    viewModel: WmtsViewModel,
    onBoardingViewModel: WmtsOnBoardingViewModel,
    onShowLayerOverlay: (WmtsSource) -> Unit,
    onMenuClick: () -> Unit,
    onGoToShop: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val topBarState by viewModel.topBarState.collectAsState()
    val onBoardingState by onBoardingViewModel.onBoardingState
    val wmtsSource by viewModel.wmtsSourceState.collectAsState()
    val hasExtendedOffer by viewModel.hasExtendedOffer.collectAsState()

    LaunchedEffectWithLifecycle(flow = viewModel.locationFlow) {
        viewModel.onLocationReceived(it)
    }

    LaunchedEffectWithLifecycle(viewModel.tileStreamProviderFlow) {
        viewModel.onNewTileStreamProvider(it)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val ok = stringResource(id = R.string.ok_dialog)
    val outOfBounds = stringResource(id = R.string.mapcreate_out_of_bounds)
    val outSideOfCoveredArea = stringResource(id = R.string.place_outside_of_covered_area)
    val awaitingLocation = stringResource(id = R.string.awaiting_location)
    var showTrekmeExtendedAdvert by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffectWithLifecycle(viewModel.events) { event ->
        val message = when (event) {
            WmtsEvent.CURRENT_LOCATION_OUT_OF_BOUNDS -> outOfBounds
            WmtsEvent.PLACE_OUT_OF_BOUNDS -> outSideOfCoveredArea
            WmtsEvent.AWAITING_LOCATION -> awaitingLocation
            WmtsEvent.SHOW_TREKME_EXTENDED_ADVERT -> {
                showTrekmeExtendedAdvert = true
                return@LaunchedEffectWithLifecycle
            }
        }

        /* Dismiss the currently showing snackbar, if any */
        snackbarHostState.currentSnackbarData?.dismiss()

        scope.launch {
            snackbarHostState.showSnackbar(message, actionLabel = ok)
        }
    }

    if (showTrekmeExtendedAdvert) {
        AdvertTrekmeExtendedDialog(
            onDismissRequest = { showTrekmeExtendedAdvert = false },
            onSeeOffer = onGoToShop
        )
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.also {
                viewModel.onTrackImport(uri)
            }
        }

    var primaryLayerSelectionData by rememberSaveable {
        mutableStateOf<PrimaryLayerSelectionData?>(
            null
        )
    }
    primaryLayerSelectionData?.also { data ->
        PrimaryLayerDialogStateful(
            layerIdsAndAvailability = data.layerIdsAndAvailability,
            initialActiveLayerId = data.selectedLayerId,
            onLayerSelected = {
                viewModel.onPrimaryLayerDefined(it)
                primaryLayerSelectionData = null
            },
            onDismiss = { primaryLayerSelectionData = null }
        )
    }

    var levelsDialogData by rememberSaveable { mutableStateOf<DownloadFormData?>(null) }
    levelsDialogData?.also { data ->
        LevelsDialogStateful(
            minLevel = data.levelMin,
            maxLevel = data.levelMax,
            p1 = data.p1,
            p2 = data.p2,
            onDownloadClicked = { minLevel, maxLevel ->
                viewModel.onDownloadFormConfirmed(minLevel, maxLevel)
                levelsDialogData = null
            },
            onDismiss = { levelsDialogData = null }
        )
    }

    val onValidateArea = {
        val downloadForm = viewModel.onValidateArea()
        levelsDialogData = downloadForm
    }

    val onPrimaryLayerSelection = l@{
        val source = wmtsSource ?: return@l
        val layers = viewModel.getAvailablePrimaryLayersForSource(source) ?: return@l
        val activeLayer = viewModel.getActivePrimaryLayerForSource(source) ?: return@l

        fun getLayerAvailability(layer: Layer): Boolean {
            if (hasExtendedOffer) return true
            return if (layer is OsmLayer) {
                when (layer) {
                    OsmAndHd, Outdoors -> false
                    else -> true
                }
            } else true
        }

        primaryLayerSelectionData = PrimaryLayerSelectionData(
            layers.map { layer -> layer.id to getLayerAvailability(layer) },
            activeLayer.id
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        WmtsScaffold(
            snackbarHostState = snackbarHostState,
            topBarState = topBarState,
            uiState = uiState,
            onToggleArea = {
                viewModel.toggleArea()
                onBoardingViewModel.onFabTipAck()
            },
            onValidateArea = onValidateArea,
            onMenuClick = onMenuClick,
            onSearchClick = viewModel::onSearchClick,
            onCloseSearch = viewModel::onCloseSearch,
            onQueryTextSubmit = viewModel::onQueryTextSubmit,
            onGeoPlaceSelection = viewModel::moveToPlace,
            onLayerSelection = onPrimaryLayerSelection,
            onZoomOnPosition = {
                viewModel.zoomOnPosition()
                onBoardingViewModel.onCenterOnPosTipAck()
            },
            onShowLayerOverlay = {
                wmtsSource?.also { onShowLayerOverlay(it) }
            },
            onUseTrack = {
                launcher.launch("*/*")
            },
            onNavigateToShop = onGoToShop
        )

        OnBoardingOverlay(
            onBoardingState,
            wmtsSource,
            onBoardingViewModel::onCenterOnPosTipAck,
            onBoardingViewModel::onFabTipAck
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.OnBoardingOverlay(
    onBoardingState: OnBoardingState,
    wmtsSource: WmtsSource?,
    onCenterOnPosAck: () -> Unit,
    onFabAck: () -> Unit
) {
    if (onBoardingState !is ShowTip) return

    val radius = with(LocalDensity.current) { 10.dp.toPx() }
    val nubWidth = with(LocalDensity.current) { 20.dp.toPx() }
    val nubHeight = with(LocalDensity.current) { 18.dp.toPx() }

    if (onBoardingState.fabTip) {
        OnBoardingTip(
            modifier = Modifier
                .width(min(maxWidth * 0.9f, 330.dp))
                .padding(bottom = 16.dp, end = 85.dp)
                .align(Alignment.BottomEnd),
            text = stringResource(id = R.string.onboarding_select_area),
            popupOrigin = PopupOrigin.BottomEnd,
            onAcknowledge = onFabAck,
            shape = DialogShape(
                radius,
                DialogShape.NubPosition.RIGHT,
                0.66f,
                nubWidth = nubWidth,
                nubHeight = nubHeight,
            )
        )
    }
    if (onBoardingState.centerOnPosTip) {
        val offset = with(LocalDensity.current) { 15.dp.toPx() }
        /* When view IGN content, there a menu button which shifts the center-on-position button */
        val relativePosition = if (wmtsSource == WmtsSource.IGN) 0.72f else 0.845f

        OnBoardingTip(
            modifier = Modifier
                .width(min(maxWidth * 0.8f, 310.dp))
                .padding(top = 60.dp, end = 50.dp)
                .align(Alignment.TopEnd),
            text = stringResource(id = R.string.onboarding_center_on_pos),
            popupOrigin = PopupOrigin.TopEnd,
            onAcknowledge = onCenterOnPosAck,
            shape = DialogShape(
                radius,
                DialogShape.NubPosition.TOP,
                relativePosition,
                nubWidth = nubWidth,
                nubHeight = nubHeight,
                offset = offset
            )
        )
    }
}

@Composable
private fun WmtsScaffold(
    snackbarHostState: SnackbarHostState,
    topBarState: TopBarState,
    uiState: UiState,
    onToggleArea: () -> Unit,
    onValidateArea: () -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCloseSearch: () -> Unit,
    onQueryTextSubmit: (String) -> Unit,
    onGeoPlaceSelection: (GeoPlace) -> Unit,
    onLayerSelection: () -> Unit,
    onZoomOnPosition: () -> Unit,
    onShowLayerOverlay: () -> Unit,
    onUseTrack: () -> Unit,
    onNavigateToShop: () -> Unit
) {
    Scaffold(
        Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            WmtsAppBar(
                topBarState,
                onSearchClick,
                onCloseSearch,
                onMenuClick,
                onQueryTextSubmit,
                onZoomOnPosition,
                onShowLayerOverlay,
                onUseTrack,
                onNavigateToShop
            )
        },
        floatingActionButton = {
            when (uiState) {
                is GeoplaceList -> {
                }

                is Wmts -> {
                    if (uiState.wmtsState is MapReady || uiState.wmtsState is AreaSelection) {
                        FabAreaSelection(onToggleArea)
                    }
                }
            }
        },
    ) { innerPadding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (uiState) {
            is GeoplaceList -> {
                /* In this context, intercept physical back gesture */
                BackHandler(onBack = onCloseSearch)

                GeoPlaceListUI(
                    modifier,
                    uiState,
                    onGeoPlaceSelection
                )
            }

            is Wmts -> {
                WmtsUI(
                    modifier,
                    uiState.wmtsState,
                    hasPrimaryLayers = (topBarState is Collapsed) && topBarState.hasPrimaryLayers,
                    onValidateArea,
                    onLayerSelection
                )
            }
        }
    }
}

@Composable
private fun WmtsUI(
    modifier: Modifier,
    wmtsState: WmtsState,
    hasPrimaryLayers: Boolean,
    onValidateArea: () -> Unit,
    onLayerSelection: () -> Unit
) {
    when (wmtsState) {
        is MapReady -> {
            Box(modifier) {
                MapUI(state = wmtsState.mapState)

                if (hasPrimaryLayers) {
                    SmallFloatingActionButton(
                        onClick = onLayerSelection,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.layers),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentDescription = null
                        )
                    }
                }
            }
        }

        is Loading -> {
            LoadingScreen()
        }

        is AreaSelection -> {
            AreaSelectionScreen(modifier, wmtsState, onValidateArea)
        }

        is WmtsError -> {
            CustomErrorScreen(wmtsState)
        }
    }
}

@Composable
private fun AreaSelectionScreen(
    modifier: Modifier,
    state: AreaSelection,
    onValidateArea: () -> Unit
) {
    Box(modifier) {
        MapUI(state = state.mapState) {
            val mapState = state.mapState
            Area(
                modifier = Modifier,
                mapState = mapState,
                backgroundColor = colorResource(id = R.color.colorBackgroundAreaView),
                strokeColor = colorResource(id = R.color.colorStrokeAreaView),
                p1 = with(state.areaUiController) {
                    Offset(
                        (p1x * mapState.fullSize.width).toFloat(),
                        (p1y * mapState.fullSize.height).toFloat()
                    )
                },
                p2 = with(state.areaUiController) {
                    Offset(
                        (p2x * mapState.fullSize.width).toFloat(),
                        (p2y * mapState.fullSize.height).toFloat()
                    )
                }
            )
        }

        Button(
            onClick = onValidateArea,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp)
        ) {
            Text(text = stringResource(id = R.string.validate_area).uppercase())
        }
    }
}


@Composable
private fun FabAreaSelection(onToggleArea: () -> Unit) {
    FloatingActionButton(
        onClick = onToggleArea,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_crop_free_white_24dp),
            contentDescription = null
        )
    }
}

@Composable
private fun Area(
    modifier: Modifier,
    mapState: MapState,
    backgroundColor: Color,
    strokeColor: Color,
    p1: Offset,
    p2: Offset
) {
    DefaultCanvas(
        modifier = modifier,
        mapState = mapState
    ) {
        val topLeft = Offset(min(p1.x, p2.x), min(p1.y, p2.y))

        drawRect(
            backgroundColor,
            topLeft = topLeft,
            size = Size(abs(p2.x - p1.x), abs(p2.y - p1.y))
        )
        drawRect(
            strokeColor, topLeft = topLeft, size = Size(abs(p2.x - p1.x), abs(p2.y - p1.y)),
            style = Stroke(width = 1.dp.toPx() / mapState.scale)
        )
    }
}

@Composable
private fun CustomErrorScreen(state: WmtsError) {
    val message = when (state) {
        WmtsError.IGN_OUTAGE -> stringResource(id = R.string.mapcreate_warning_ign)
        WmtsError.VPS_FAIL -> stringResource(id = R.string.mapreate_warning_vps)
        WmtsError.PROVIDER_OUTAGE -> stringResource(id = R.string.mapcreate_warning_others)
    }
    ErrorScreen(message = message)
}