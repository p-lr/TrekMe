package com.peterlaurence.trekme.features.excursionsearch.presentation.ui.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peterlaurence.trekme.R
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
import com.peterlaurence.trekme.core.wmts.domain.model.UsgsData
import com.peterlaurence.trekme.core.wmts.domain.model.WorldStreetMap
import com.peterlaurence.trekme.features.common.presentation.ui.bottomsheet.CollapsibleBottomSheet
import com.peterlaurence.trekme.features.common.presentation.ui.bottomsheet.States
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.ConfirmDialog
import com.peterlaurence.trekme.features.common.presentation.ui.screens.ErrorScreen
import com.peterlaurence.trekme.features.common.presentation.ui.screens.LoadingScreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.excursionsearch.presentation.ui.component.ElevationGraph
import com.peterlaurence.trekme.features.excursionsearch.presentation.ui.component.ElevationGraphPoint
import com.peterlaurence.trekme.features.excursionsearch.presentation.ui.dialog.MapSourceDataSelect
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.AwaitingLocation
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.Error
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.ExcursionMapViewModel
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.Loading
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.MapReady
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.UiState
import com.peterlaurence.trekme.util.ResultL
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import com.peterlaurence.trekme.util.fold
import com.peterlaurence.trekme.util.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.setVisibleAreaPadding
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.UUID

@Composable
fun ExcursionMapStateful(
    viewModel: ExcursionMapViewModel,
    onBack: () -> Unit,
    onGoToMapList: () -> Unit,
    onGoToShop: () -> Unit
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val mapSourceData by viewModel.mapSourceDataFlow.collectAsStateWithLifecycle()
    val hasExtendedOffer by viewModel.extendedOfferFlow.collectAsState(initial = false)
    val swipeableState = rememberSwipeableState(initialValue = States.COLLAPSED)
    val geoRecordForSearchState by viewModel.geoRecordForSearchFlow.collectAsStateWithLifecycle()
    val hasContainingMap by viewModel.routeLayer.hasContainingMap.collectAsStateWithLifecycle()
    var isDownloadOptionChecked by remember { mutableStateOf(!hasContainingMap) }

    val bottomSheetDataState: ResultL<BottomSheetData> by produceState(
        initialValue = ResultL.loading(),
        key1 = geoRecordForSearchState,
        key2 = isDownloadOptionChecked
    ) {
        geoRecordForSearchState.map { geoRecordForSearchItem ->
            value = ResultL.success(
                makeBottomSheetData(
                    geoRecordForSearchItem.geoRecord,
                    isDownloadOptionChecked
                )
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val noInternetWarning = stringResource(id = R.string.no_internet_excursions)
    val excursionDownloadStart = stringResource(id = R.string.excursion_download_start)
    val excursionDownloadError = stringResource(id = R.string.excursion_download_error)
    var isShowingMapDownloadDialog by remember { mutableStateOf(false) }
    LaunchedEffectWithLifecycle(flow = viewModel.event) { event ->
        when (event) {
            ExcursionMapViewModel.Event.OnMarkerClick -> {
                swipeableState.snapTo(States.EXPANDED)
            }

            ExcursionMapViewModel.Event.NoInternet -> {
                snackbarHostState.showSnackbar(
                    message = noInternetWarning,
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite
                )
            }

            ExcursionMapViewModel.Event.ExcursionDownloadStart -> {
                if (!isDownloadOptionChecked) {
                    snackbarHostState.showSnackbar(
                        message = excursionDownloadStart,
                        withDismissAction = true,
                        duration = SnackbarDuration.Long
                    )
                }
            }

            ExcursionMapViewModel.Event.ExcursionDownloadError -> {
                snackbarHostState.showSnackbar(
                    message = excursionDownloadError,
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    LaunchedEffectWithLifecycle(flow = viewModel.locationFlow) {
        viewModel.onLocationReceived(it)
    }

    /* Handle map padding and center on georecord if bottomsheet is expanded. */
    LaunchedEffect(swipeableState.currentValue, uiState, geoRecordForSearchState) {
        val mapState = (uiState as? MapReady)?.mapState ?: return@LaunchedEffect
        val geoRecord = geoRecordForSearchState.getOrNull()?.geoRecord ?: return@LaunchedEffect

        val paddingRatio = when (swipeableState.currentValue) {
            States.EXPANDED, States.PEAKED -> expandedRatio
            States.COLLAPSED -> 0f
        }
        launch {
            mapState.setVisibleAreaPadding(bottomRatio = paddingRatio)
            if (swipeableState.currentValue == States.EXPANDED) {
                viewModel.routeLayer.centerOnGeoRecord(mapState, geoRecord.id)
            }
        }
    }

    var isShowingLayerSelectionDialog by rememberSaveable { mutableStateOf(false) }

    ExcursionMapScreen(
        uiState = uiState,
        swipeableState = swipeableState,
        bottomSheetDataState = bottomSheetDataState,
        snackbarHostState = snackbarHostState,
        onCursorMove = { latLon, d, ele ->
            viewModel.routeLayer.setCursor(latLon, distance = d, ele = ele)
        },
        onToggleDownloadMapOption = { isDownloadOptionChecked = !isDownloadOptionChecked },
        onDownload = {
            if (isDownloadOptionChecked) {
                isShowingMapDownloadDialog = true
            }
            viewModel.onDownload(isDownloadOptionChecked)
        },
        onBack = onBack,
        onLayerSelection = { isShowingLayerSelectionDialog = true }
    )

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
    swipeableState: SwipeableState<States>,
    bottomSheetDataState: ResultL<BottomSheetData>,
    snackbarHostState: SnackbarHostState,
    onCursorMove: (latLon: LatLon, d: Double, ele: Double) -> Unit = { _, _, _ -> },
    onToggleDownloadMapOption: () -> Unit,
    onDownload: () -> Unit,
    onBack: () -> Unit,
    onLayerSelection: () -> Unit
) {
    Scaffold(
        topBar = {
            ExcursionMapTopAppBar(
                onBack = onBack,
            )
        },
        bottomBar = {
            if (swipeableState.currentValue != States.COLLAPSED) {
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

            Loading -> {
                LoadingScreen()
            }

            AwaitingLocation -> {
                LoadingScreen(stringResource(id = R.string.awaiting_location))
            }

            is MapReady -> {
                Box(modifier) {
                    ExcursionMap(
                        mapState = uiState.mapState,
                        onLayerSelection = onLayerSelection
                    )
                    BottomSheet(
                        swipeableState,
                        bottomSheetDataState,
                        onCursorMove,
                        onToggleDownloadMapOption
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomSheet(
    swipeableState: SwipeableState<States>,
    bottomSheetDataState: ResultL<BottomSheetData>,
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
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                },
                onSuccess = { data ->
                    statisticsSection(data)
                    elevationGraphSection(data, onCursorMove)
                    downloadSection(data.isDownloadOptionChecked, onToggleDownloadMapOption)
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
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

private fun LazyListScope.downloadSection(
    isChecked: Boolean,
    onToggleDownloadMapOption: () -> Unit
) {
    item {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleDownloadMapOption)
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggleDownloadMapOption() })
            Text(text = stringResource(id = R.string.download_map_option))
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
    onLayerSelection: () -> Unit
) {
    Box {
        MapUI(modifier = modifier, state = mapState)

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

    return BottomSheetData(geoRecord.id, stats, elevationGraphPoints, isDownloadOptionChecked)
}

private data class BottomSheetData(
    val id: UUID,
    val geoStatistics: GeoStatistics,
    val elevationGraphPoints: List<ElevationGraphPoint>?,
    val isDownloadOptionChecked: Boolean
)

@Preview(showBackground = true, widthDp = 450, heightDp = 600)
@Composable
private fun BottomSheetPreview() {
    val geoStats = GeoStatistics(
        distance = 1527.0,
        elevationMax = 2600.0,
        elevationMin = 2200.0,
        elevationUpStack = 550.0,
        elevationDownStack = 451.0,
        durationInSecond = 11658,
        avgSpeed = null
    )

    TrekMeTheme {
        val swipeableState = rememberSwipeableState(initialValue = States.PEAKED)
        val bottomSheetData by remember {
            mutableStateOf(
                ResultL.success(
                    BottomSheetData(UUID.randomUUID(), geoStats, null, true)
                )
            )
        }

        Scaffold { paddingValues ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                BottomSheet(
                    swipeableState = swipeableState,
                    bottomSheetDataState = bottomSheetData
                )
            }
        }
    }

}

private const val expandedRatio = 0.6f
private const val peakedRatio = 0.2f