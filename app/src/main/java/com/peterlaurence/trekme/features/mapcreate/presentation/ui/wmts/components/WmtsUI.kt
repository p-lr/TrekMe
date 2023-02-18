package com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.features.common.presentation.ui.screens.ErrorScreen
import com.peterlaurence.trekme.features.common.presentation.ui.screens.LoadingScreen
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.DialogShape
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.OnBoardingTip
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.PopupOrigin
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.dialogs.LevelsDialogStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.dialogs.PrimaryLayerDialogStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.DownloadFormData
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.PrimaryLayerSelectionData
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.DefaultCanvas
import ovh.plrapps.mapcompose.api.fullSize
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.abs
import kotlin.math.min

@Composable
fun WmtsUI(
    modifier: Modifier,
    wmtsState: WmtsState,
    onValidateArea: () -> Unit
) {
    when (wmtsState) {
        is MapReady -> {
            MapUI(modifier, state = wmtsState.mapState)
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

@Composable
fun WmtsStateful(
    viewModel: WmtsViewModel,
    onBoardingViewModel: WmtsOnBoardingViewModel,
    onShowLayerOverlay: () -> Unit,
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val topBarState by viewModel.topBarState.collectAsState()
    val onBoardingState by onBoardingViewModel.onBoardingState
    val wmtsSource by viewModel.wmtsSourceState.collectAsState()

    val events = viewModel.eventListState.toList()

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
            layerIds = data.layerIds,
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

        primaryLayerSelectionData = PrimaryLayerSelectionData(layers.map { it.id }, activeLayer.id)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        WmtsScaffold(
            events,
            topBarState,
            uiState,
            viewModel::acknowledgeError,
            onToggleArea = {
                viewModel.toggleArea()
                onBoardingViewModel.onFabTipAck()
            },
            onValidateArea,
            onMenuClick,
            viewModel::onSearchClick,
            viewModel::onCloseSearch,
            viewModel::onQueryTextSubmit,
            viewModel::moveToPlace,
            onPrimaryLayerSelection,
            onZoomOnPosition = {
                viewModel.zoomOnPosition()
                onBoardingViewModel.onCenterOnPosTipAck()
            },
            onShowLayerOverlay,
            onUseTrack = {
                launcher.launch("*/*")
            }
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
                .padding(top = 60.dp, end = 16.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WmtsScaffold(
    events: List<WmtsEvent>, topBarState: TopBarState, uiState: UiState,
    onAckError: () -> Unit,
    onToggleArea: () -> Unit,
    onValidateArea: () -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit, onCloseSearch: () -> Unit,
    onQueryTextSubmit: (String) -> Unit,
    onGeoPlaceSelection: (GeoPlace) -> Unit,
    onLayerSelection: () -> Unit,
    onZoomOnPosition: () -> Unit,
    onShowLayerOverlay: () -> Unit,
    onUseTrack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    if (events.isNotEmpty()) {
        val ok = stringResource(id = R.string.ok_dialog)
        val message = when (events.first()) {
            WmtsEvent.CURRENT_LOCATION_OUT_OF_BOUNDS -> stringResource(id = R.string.mapcreate_out_of_bounds)
            WmtsEvent.PLACE_OUT_OF_BOUNDS -> stringResource(id = R.string.place_outside_of_covered_area)
        }

        SideEffect {
            scope.launch {
                /* Dismiss the currently showing snackbar, if any */
                snackbarHostState.currentSnackbarData?.dismiss()

                snackbarHostState.showSnackbar(message, actionLabel = ok)
            }
            onAckError()
        }
    }

    Scaffold(
        Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            SearchAppBar(
                topBarState,
                onSearchClick,
                onCloseSearch,
                onMenuClick,
                onQueryTextSubmit,
                onLayerSelection,
                onZoomOnPosition,
                onShowLayerOverlay,
                onUseTrack
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
                    onValidateArea,
                )
            }
        }
    }
}