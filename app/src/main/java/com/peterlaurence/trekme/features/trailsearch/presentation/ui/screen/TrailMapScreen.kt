package com.peterlaurence.trekme.features.trailsearch.presentation.ui.screen

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.excursion.domain.model.TrailSearchItem
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.core.georecord.domain.logic.getGeoStatistics
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.core.location.domain.model.LatLon
import com.peterlaurence.trekme.core.units.UnitFormatter.formatDistance
import com.peterlaurence.trekme.core.units.UnitFormatter.formatDuration
import com.peterlaurence.trekme.core.wmts.domain.model.IgnClassic
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSpainData
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.OpenTopoMap
import com.peterlaurence.trekme.core.wmts.domain.model.OrdnanceSurveyData
import com.peterlaurence.trekme.core.wmts.domain.model.OsmSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.SwissTopoData
import com.peterlaurence.trekme.core.wmts.domain.model.TILE_SIZE_IN_MO
import com.peterlaurence.trekme.core.wmts.domain.model.UsgsData
import com.peterlaurence.trekme.core.wmts.domain.model.WorldStreetMap
import com.peterlaurence.trekme.features.common.presentation.ui.bottomsheet.CollapsibleBottomSheet
import com.peterlaurence.trekme.features.common.presentation.ui.bottomsheet.States
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.ConfirmDialog
import com.peterlaurence.trekme.features.common.presentation.ui.screens.ErrorScreen
import com.peterlaurence.trekme.features.common.presentation.ui.screens.LoadingScreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.OnBoardingTip
import com.peterlaurence.trekme.features.trailsearch.presentation.model.GeoPlaceAndDistance
import com.peterlaurence.trekme.features.trailsearch.presentation.ui.component.ElevationGraph
import com.peterlaurence.trekme.features.trailsearch.presentation.ui.component.ElevationGraphPoint
import com.peterlaurence.trekme.features.trailsearch.presentation.ui.component.GeoPlaceListComponent
import com.peterlaurence.trekme.features.trailsearch.presentation.ui.dialog.MapSourceDataSelect
import com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.TrailMapViewModel.Event
import com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.AwaitingLocation
import com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.DownloadNotAllowed
import com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.DownloadNotAllowedReason
import com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.Error
import com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.TrailMapViewModel
import com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.Loading
import com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.LoadingLayer
import com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.MapDownloadData
import com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.MapDownloadState
import com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.MapReady
import com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.UiState
import com.peterlaurence.trekme.util.ResultL
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import com.peterlaurence.trekme.util.fold
import com.peterlaurence.trekme.util.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setMapBackground
import ovh.plrapps.mapcompose.api.setVisibleAreaPadding
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.UUID
import kotlin.math.roundToInt


@Composable
fun TrailMapStateful(
    viewModel: TrailMapViewModel = hiltViewModel(),
    onGoToMapList: () -> Unit,
    onGoToShop: () -> Unit,
    onGoToMapCreation: () -> Unit
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val geoplaceList by viewModel.geoPlaceFlow.collectAsStateWithLifecycle()
    val isGeoPlaceLoading by viewModel.isGeoPlaceLoading.collectAsStateWithLifecycle()
    val isTrailUpdatePending by viewModel.isTrailUpdatePending.collectAsStateWithLifecycle()
    val isShowingHelperTip by viewModel.isShowingHelperTip.collectAsStateWithLifecycle()
    val mapSourceData by viewModel.mapSourceDataFlow.collectAsStateWithLifecycle()
    val hasExtendedOffer by viewModel.extendedOfferFlow.collectAsState(initial = false)
    val swipeableState = rememberSwipeableState(initialValue = States.COLLAPSED)
    val geoRecordForBottomSheet by viewModel.geoRecordForBottomSheet.collectAsStateWithLifecycle()
    val mapDownloadState by viewModel.mapDownloadStateFlow.collectAsStateWithLifecycle()
    var isDownloadOptionChecked by remember(mapDownloadState) {
        mutableStateOf(!((mapDownloadState as? MapDownloadData)?.hasContainingMap ?: true))
    }

    val bottomSheetDataState by produceState<ResultL<BottomSheetData?>>(
        /* Do not use loading state at init as it triggers an indeterminate progress bar.
         * In this context, null expresses an uninitialized state */
        initialValue = ResultL.success(null),
        key1 = geoRecordForBottomSheet,
        key2 = mapDownloadState,
        key3 = isDownloadOptionChecked
    ) {
        value = geoRecordForBottomSheet.map { geoRecordForBottomsheet ->
            if (geoRecordForBottomsheet != null) {
                makeBottomSheetData(
                    geoRecordForBottomsheet.geoRecord,
                    mapDownloadState,
                    isDownloadOptionChecked
                )
            } else null
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val noInternetWarning = stringResource(id = R.string.no_internet_excursions)
    val excursionDownloadStart = stringResource(id = R.string.excursion_download_start)
    val excursionDownloadError = stringResource(id = R.string.excursion_download_error)
    val excursionSearchError = stringResource(id = R.string.excursion_download_error)
    val outSideOfCoveredArea = stringResource(id = R.string.place_outside_of_covered_area)
    var isShowingMapDownloadDialog by remember { mutableStateOf(false) }
    var isShowingTrailSelectionDialog by rememberSaveable {
        mutableStateOf<List<Pair<TrailSearchItem, Color>>?>(null)
    }

    LaunchedEffectWithLifecycle(flow = viewModel.event) { event ->
        when (event) {
            is Event.OnMarkerClick -> {
                swipeableState.snapTo(States.EXPANDED)
            }

            is Event.OnTrailClick -> {
                swipeableState.snapTo(States.EXPANDED)
            }

            is Event.NoInternet -> {
                showSnackbar(snackbarHostState, noInternetWarning)
            }

            is Event.ExcursionOnlyDownloadStart -> {
                showSnackbar(snackbarHostState, excursionDownloadStart)
            }

            is Event.ExcursionDownloadError -> {
                showSnackbar(snackbarHostState, excursionDownloadError)
            }

            is Event.SearchError -> {
                showSnackbar(snackbarHostState, excursionSearchError)
            }

            is Event.PlaceOutOfBounds -> {
                showSnackbar(snackbarHostState, outSideOfCoveredArea)
            }

            is Event.MultipleTrailClicked -> {
                isShowingTrailSelectionDialog = event.tracks
            }
        }
    }

    LaunchedEffectWithLifecycle(flow = viewModel.locationFlow) {
        viewModel.onLocationReceived(it)
    }

    /* Handle map padding and center on georecord if bottomsheet is expanded. */
    LaunchedEffect(swipeableState.currentValue, uiState, geoRecordForBottomSheet) {
        val mapState = (uiState as? MapReady)?.mapState ?: return@LaunchedEffect
        val bb = geoRecordForBottomSheet.getOrNull()?.boundingBoxNormalized ?: return@LaunchedEffect

        val paddingRatio = when (swipeableState.currentValue) {
            States.EXPANDED, States.PEAKED -> expandedRatio
            States.COLLAPSED -> {
                viewModel.resetTrail()
                0f
            }
        }
        launch {
            mapState.setVisibleAreaPadding(bottomRatio = paddingRatio)
            if (swipeableState.currentValue == States.EXPANDED) {
                mapState.scrollTo(bb, Offset(0.2f, 0.2f))
            }
        }
    }

    var isShowingLayerSelectionDialog by rememberSaveable { mutableStateOf(false) }

    ExcursionMapScreen(
        uiState = uiState,
        geoplaceList = geoplaceList,
        isGeoPlaceLoading = isGeoPlaceLoading,
        isTrailUpdatePending = isTrailUpdatePending,
        isShowingHelperTip = isShowingHelperTip,
        swipeableState = swipeableState,
        bottomSheetDataState = bottomSheetDataState,
        snackbarHostState = snackbarHostState,
        onLocationSearch = viewModel::onLocationSearch,
        onGeoPlaceSelection = viewModel::centerOnPlace,
        onCursorMove = { latLon, d, ele ->
            viewModel.calloutLayer.setCursor(latLon, distance = d, ele = ele)
        },
        onToggleDownloadMapOption = { isDownloadOptionChecked = !isDownloadOptionChecked },
        onDownload = {
            if (isDownloadOptionChecked) {
                isShowingMapDownloadDialog = true
            }
            viewModel.onDownload(isDownloadOptionChecked)
        },
        onLayerSelection = { isShowingLayerSelectionDialog = true },
        onGoToMapCreation = onGoToMapCreation,
        onTipAck = { viewModel.ackTip() }
    )

    isShowingTrailSelectionDialog?.also { data ->
        AlertDialog(
            onDismissRequest = { isShowingTrailSelectionDialog = null },
            confirmButton = {},
            title = {
                Text(text = stringResource(id = R.string.trail_select_disalog_title))
            },
            text = {
                Column {
                    data.forEachIndexed { index, it ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectTrail(
                                        it.first.id,
                                        it.first.name,
                                        it.first.group
                                    )
                                    isShowingTrailSelectionDialog = null
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(it.second)
                            )

                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = it.first.name ?: "")
                        }

                        if (index < data.size - 1 ) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        )
    }

    if (isShowingMapDownloadDialog) {
        ConfirmDialog(
            contentText = stringResource(id = R.string.excursion_map_download),
            confirmButtonText = stringResource(id = R.string.ok_dialog),
            cancelButtonText = stringResource(id = R.string.no_dialog),
            onDismissRequest = { isShowingMapDownloadDialog = false },
            onConfirmPressed = onGoToMapList,
        )
    }

    if (isShowingLayerSelectionDialog) {
        var selection by remember(mapSourceData) { mutableStateOf(mapSourceData) }

        val requiresExtendedOffer: (MapSourceData) -> Boolean = remember(hasExtendedOffer) {
            {
                !hasExtendedOffer && viewModel.requiresOffer(it)
            }
        }

        MapSourceDataSelect(
            mapSourceDataList = listOf(
                OsmSourceData(WorldStreetMap),
                OsmSourceData(OpenTopoMap),
                IgnSourceData(IgnClassic, overlays = emptyList()),
                UsgsData,
                OrdnanceSurveyData,
                IgnSpainData,
                SwissTopoData
            ),
            currentMapSourceData = selection,
            onMapSourceDataSelected = {
                if (!requiresExtendedOffer(it)) {
                    isShowingLayerSelectionDialog = false
                    viewModel.onMapSourceDataChange(it)
                }
                selection = it
            },
            requiresExtendedOffer = requiresExtendedOffer,
            onDismiss = {
                isShowingLayerSelectionDialog = false
            },
            onSeeOffer = onGoToShop
        )
    }
}

@Composable
private fun ExcursionMapScreen(
    uiState: UiState,
    geoplaceList: List<GeoPlaceAndDistance>,
    isGeoPlaceLoading: Boolean,
    isTrailUpdatePending: Boolean,
    isShowingHelperTip: Boolean,
    swipeableState: SwipeableState<States>,
    bottomSheetDataState: ResultL<BottomSheetData?>,
    snackbarHostState: SnackbarHostState,
    onLocationSearch: (String) -> Unit,
    onGeoPlaceSelection: (GeoPlace) -> Unit,
    onCursorMove: (latLon: LatLon, d: Double, ele: Double) -> Unit = { _, _, _ -> },
    onToggleDownloadMapOption: () -> Unit = {},
    onDownload: () -> Unit = {},
    onLayerSelection: () -> Unit = {},
    onGoToMapCreation: () -> Unit = {},
    onTipAck: () -> Unit = {}
) {
    var isInSearchMode by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        bottomBar = {
            val bottomSheetDataSuccess = bottomSheetDataState.getOrNull()
            if (
                swipeableState.currentValue != States.COLLAPSED
                && bottomSheetDataSuccess != null
                && bottomSheetDataSuccess.mapDownloadState != Loading
                && !isInSearchMode
            ) {
                Row(
                    modifier = Modifier
                        .height(58.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onDownload) {
                        Text(stringResource(id = R.string.download))
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        when (uiState) {
            Error.PROVIDER_OUTAGE -> {
                ErrorScreen(message = stringResource(id = R.string.provider_issue))
            }

            // TODO: remove?
            Error.NO_EXCURSIONS -> {
                EscapeHatchScreen(modifier, onGoToMapCreation)
            }

            AwaitingLocation -> {
                LoadingScreen(stringResource(id = R.string.awaiting_location))
            }

            is MapReady -> {
                if (isInSearchMode) {
                    BackHandler {
                        isInSearchMode = false
                    }
                    Column(modifier) {
                        var searchText by rememberSaveable {
                            mutableStateOf("")
                        }
                        Surface(
                            Modifier
                                .padding(start = 12.dp, end = 12.dp)
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .height(40.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(50)
                                ),
                            shape = RoundedCornerShape(50),
                        ) {
                            val focusRequester = remember { FocusRequester() }
                            BasicTextField(
                                modifier = Modifier.focusRequester(focusRequester),
                                value = searchText,
                                onValueChange = {
                                    searchText = it
                                    onLocationSearch(it)
                                },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            ) { innerTextField ->
                                Row(
                                    modifier,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Leading icon
                                    IconButton(onClick = { isInSearchMode = false }) {
                                        Image(
                                            painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                            contentDescription = null
                                        )
                                    }

                                    Box(
                                        Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchText.isEmpty()) {
                                            Text(
                                                stringResource(id = R.string.excursion_search_button),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 16.sp
                                            )
                                        }
                                        innerTextField()
                                    }

                                    // Trailing icon
                                    IconButton(onClick = { searchText = "" }) {
                                        Image(
                                            painter = painterResource(id = R.drawable.close_circle_outline),
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                            contentDescription = null
                                        )
                                    }
                                }
                            }

                            DisposableEffect(Unit) {
                                focusRequester.requestFocus()
                                onDispose { }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        GeoPlaceListComponent(
                            geoPlaceList = geoplaceList,
                            isLoading = isGeoPlaceLoading,
                            onGeoPlaceSelection = {
                                isInSearchMode = false
                                onGeoPlaceSelection(it)
                            }
                        )
                    }
                } else {
                    Box(modifier) {
                        ExcursionMap(
                            mapState = uiState.mapState,
                            isSearchPending = isTrailUpdatePending,
                            onSearchClick = { isInSearchMode = true },
                            onLayerSelection = onLayerSelection
                        )
                        BottomSheet(
                            swipeableState,
                            bottomSheetDataState,
                            onCursorMove,
                            onToggleDownloadMapOption
                        )

                        if (isShowingHelperTip) {
                            OnBoardingTip(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(horizontal = 12.dp, vertical = 64.dp),
                                text = stringResource(id = R.string.trail_helper_tip),
                                onAcknowledge = onTipAck
                            )
                        }
                    }
                }
            }

            LoadingLayer -> {
                LoadingScreen(stringResource(id = R.string.loading_layer))
            }
        }
    }
}

@Composable
private fun BottomSheet(
    swipeableState: SwipeableState<States>,
    bottomSheetDataState: ResultL<BottomSheetData?>,
    onCursorMove: (latLon: LatLon, d: Double, ele: Double) -> Unit = { _, _, _ -> },
    onToggleDownloadMapOption: () -> Unit = {}
) {
    CollapsibleBottomSheet(
        swipeableState = swipeableState,
        expandedRatio = expandedRatio,
        peakedRatio = peakedRatio,
        content = {
            bottomSheetDataState.fold(
                onLoading = {
                    item {
                        Box(Modifier.fillMaxWidth()) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(vertical = 32.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                },
                onSuccess = { data ->
                    if (data != null) {
                        headerSection(data.title)
                        statisticsSection(data)
                        elevationGraphSection(data, onCursorMove)
                        downloadSection(
                            data.mapDownloadState,
                            data.isDownloadOptionChecked,
                            onToggleDownloadMapOption
                        )
                    }
                },
                onFailure = {
                    item("error") {
                        Text(stringResource(id = R.string.excursion_download_error))
                    }
                }
            )
        },
    )
}

private fun LazyListScope.headerSection(title: String) {
    item("header") {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun LazyListScope.statisticsSection(data: BottomSheetData) {
    item(key = data.geoStatistics) {
        TrackStats(data.geoStatistics)
    }
}

private fun LazyListScope.elevationGraphSection(
    data: BottomSheetData,
    onCursorMove: (latLon: LatLon, d: Double, ele: Double) -> Unit
) {
    if (data.elevationGraphPoints != null) {
        item(key = "elevation-graph") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
                Text(
                    stringResource(id = R.string.maybe_unordered),
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card {
                    ElevationGraph(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        points = data.elevationGraphPoints,
                        verticalSpacingY = 20.dp,
                        verticalPadding = 16.dp,
                        onCursorMove = onCursorMove
                    )
                }
            }
        }
    }
}

private fun LazyListScope.downloadSection(
    mapDownloadState: MapDownloadState,
    isChecked: Boolean,
    onToggleDownloadMapOption: () -> Unit
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleDownloadMapOption)
        ) {
            when (mapDownloadState) {
                is DownloadNotAllowed -> {
                    Image(
                        painterResource(id = R.drawable.information),
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
                        contentDescription = null,
                    )
                    Text(
                        text = when (mapDownloadState.reason) {
                            DownloadNotAllowedReason.Restricted -> stringResource(id = R.string.restricted_download)
                            DownloadNotAllowedReason.TooBigMap -> stringResource(id = R.string.area_too_big)
                        },
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }

                is MapDownloadData -> {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleDownloadMapOption() }
                    )
                    Text(
                        text = stringResource(
                            id = R.string.download_map_option,
                            (mapDownloadState.tileCount * TILE_SIZE_IN_MO).roundToInt()
                        ),
                        modifier = Modifier.padding(end = 16.dp, top = 12.dp)
                    )
                }

                Loading -> CircularProgressIndicator(
                    Modifier
                        .padding(start = 16.dp, bottom = 16.dp)
                        .size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExcursionMapTopAppBar(
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(id = R.string.excursions)) }, // TODO: show "On foot", "Bike", etc
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(id = R.drawable.baseline_arrow_back_24), null,
                )
            }
        }
    )
}

@Composable
private fun ExcursionMap(
    modifier: Modifier = Modifier,
    mapState: MapState,
    isSearchPending: Boolean,
    onSearchClick: () -> Unit,
    onLayerSelection: () -> Unit,
) {
    Box {
        MapUI(modifier = modifier, state = mapState)

        Surface(
            Modifier
                .padding(start = 12.dp, end = 64.dp)
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(40.dp)
                .align(Alignment.TopCenter)
                .clickable(onClick = onSearchClick),
            shape = RoundedCornerShape(50),
            shadowElevation = 4.dp
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(id = R.string.excursion_search_button))
            }
        }
        if (isSearchPending) {
            CircularProgressIndicator(
                Modifier
                    .padding(top = 64.dp)
                    .size(25.dp)
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                    .padding(4.dp),
                strokeWidth = 2.dp
            )
        }

        SmallFloatingActionButton(
            onClick = onLayerSelection,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackStats(geoStatistics: GeoStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            maxItemsInEachRow = 3
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val dist = remember(geoStatistics) {
                    formatDistance(geoStatistics.distance)
                }
                Icon(
                    painter = painterResource(id = R.drawable.distance),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = dist.substringBefore(' '),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = dist.substringAfter(' '),
                        fontSize = 12.sp
                    )
                }
                Text(text = stringResource(id = R.string.distance), fontSize = 12.sp)
            }

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.clock),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = formatDuration(geoStatistics.durationInSecond ?: 0),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(text = stringResource(id = R.string.duration_stat), fontSize = 12.sp)
            }

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val verticalDrop = remember(geoStatistics) {
                    val drop =
                        (geoStatistics.elevationMax ?: 0.0) - (geoStatistics.elevationMin ?: 0.0)
                    formatDistance(drop)
                }
                Icon(
                    painter = painterResource(id = R.drawable.vertical_drop),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = verticalDrop.substringBefore(' '),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = verticalDrop.substringAfter(' '),
                        fontSize = 12.sp
                    )
                }
                Text(text = stringResource(id = R.string.vertical_drop_stat), fontSize = 12.sp)
            }

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val verticalDrop = remember(geoStatistics) {
                    formatDistance(geoStatistics.elevationUpStack)
                }
                Icon(
                    painter = painterResource(id = R.drawable.vertical_ascent),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = verticalDrop.substringBefore(' '),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = verticalDrop.substringAfter(' '),
                        fontSize = 12.sp
                    )
                }
                Text(text = stringResource(id = R.string.vertical_ascent_stat), fontSize = 12.sp)
            }

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val verticalDrop = remember(geoStatistics) {
                    formatDistance(geoStatistics.elevationDownStack)
                }
                Icon(
                    painter = painterResource(id = R.drawable.vertical_descent),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = "-" + verticalDrop.substringBefore(' '),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = verticalDrop.substringAfter(' '),
                        fontSize = 12.sp
                    )
                }
                Text(text = stringResource(id = R.string.vertical_descent_stat), fontSize = 12.sp)
            }
        }
    }
}

private suspend fun makeBottomSheetData(
    geoRecord: GeoRecord,
    mapDownloadState: MapDownloadState,
    isDownloadOptionChecked: Boolean
): BottomSheetData {
    val points = mutableListOf<ElevationGraphPoint>()
    val stats = withContext(Dispatchers.Default) {
        getGeoStatistics(geoRecord) { distance, marker ->
            marker.elevation?.also { ele ->
                points += ElevationGraphPoint(marker.lat, marker.lon, distance, ele)
            }
        }
    }
    val elevationGraphPoints = points.ifEmpty { null }
    /* For instance, we only have one source of trail, and they may be unordered when the number
     * of section is strictly greater than 1. */
    val maybeUnordered = geoRecord.routeGroups.firstOrNull()?.let {
        it.routes.size > 1
    } ?: false

    return BottomSheetData(
        id = geoRecord.id,
        title = geoRecord.name,
        geoStatistics = stats,
        elevationGraphPoints = elevationGraphPoints,
        maybeUnordered = maybeUnordered,
        mapDownloadState = mapDownloadState,
        isDownloadOptionChecked = isDownloadOptionChecked
    )
}

private data class BottomSheetData(
    val id: UUID,
    val title: String,
    val geoStatistics: GeoStatistics,
    val elevationGraphPoints: List<ElevationGraphPoint>?,
    val maybeUnordered: Boolean,
    val mapDownloadState: MapDownloadState,
    val isDownloadOptionChecked: Boolean
)

@Composable
private fun EscapeHatchScreen(
    modifier: Modifier = Modifier,
    onGoToMapCreation: () -> Unit = {}
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.desert),
            contentDescription = null,
            modifier = Modifier
                .padding(48.dp)
                .fillMaxWidth()
        )
        Text(
            text = stringResource(id = R.string.no_excursion_found),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(text = stringResource(id = R.string.no_excursion_recommendation))
        Button(
            onClick = onGoToMapCreation,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = stringResource(id = R.string.no_excursion_create_map))
        }
    }
}

private suspend fun showSnackbar(snackbarHostState: SnackbarHostState, message: String) {
    snackbarHostState.showSnackbar(
        message = message,
        withDismissAction = true,
        duration = SnackbarDuration.Indefinite
    )
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(showBackground = true)
@Composable
private fun ExcursionMapScreenPreview() {
    val swipeableState = rememberSwipeableState(initialValue = States.PEAKED)

    val mapState = remember {
        MapState(5, 10000, 10000).apply {
            setMapBackground(Color(0xffbde3f6))
        }
    }

    val mapDownloadState = MapDownloadData(hasContainingMap = true, tileCount = 1500L)

    val geoStats = remember {
        GeoStatistics(
            distance = 1527.0,
            elevationMax = 2600.0,
            elevationMin = 2200.0,
            elevationUpStack = 550.0,
            elevationDownStack = 451.0,
            durationInSecond = 11658,
            avgSpeed = null
        )
    }
    val bottomSheetData by remember {
        mutableStateOf(
            ResultL.success(
                BottomSheetData(
                    id = UUID.randomUUID(),
                    title = "Trail title",
                    geoStatistics = geoStats,
                    elevationGraphPoints = null,
                    maybeUnordered = true,
                    mapDownloadState = mapDownloadState,
                    isDownloadOptionChecked = true
                )
            )
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    TrekMeTheme {
        ExcursionMapScreen(
            uiState = MapReady(mapState),
            geoplaceList = emptyList(),
            isGeoPlaceLoading = false,
            isTrailUpdatePending = true,
            isShowingHelperTip = true,
            swipeableState = swipeableState,
            bottomSheetDataState = bottomSheetData,
            snackbarHostState = snackbarHostState,
            onLocationSearch = {},
            onGeoPlaceSelection = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 550)
@Composable
fun EscapePreview() {
    TrekMeTheme {
        EscapeHatchScreen()
    }
}

private const val expandedRatio = 0.6f
private const val peakedRatio = 0.2f