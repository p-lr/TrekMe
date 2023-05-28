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
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.peterlaurence.trekme.features.common.presentation.ui.bottomsheet.CollapsibleBottomSheet
import com.peterlaurence.trekme.features.common.presentation.ui.bottomsheet.States
import com.peterlaurence.trekme.features.common.presentation.ui.screens.ErrorScreen
import com.peterlaurence.trekme.features.common.presentation.ui.screens.LoadingScreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.excursionsearch.presentation.ui.component.ElevationGraph
import com.peterlaurence.trekme.features.excursionsearch.presentation.ui.component.ElevationGraphPoint
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcursionMapStateful(
    viewModel: ExcursionMapViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val swipeableState = rememberSwipeableState(initialValue = States.COLLAPSED)
    val geoRecordState by viewModel.geoRecordFlow.collectAsStateWithLifecycle()
    val hasContainingMap by viewModel.routeLayer.hasContainingMap.collectAsStateWithLifecycle()

    val bottomSheetDataState: ResultL<BottomSheetData> by produceState(
        initialValue = ResultL.loading(),
        key1 = geoRecordState,
        key2 = hasContainingMap
    ) {
        geoRecordState.map { geoRecord ->
            val isDownloadOptionChecked = !hasContainingMap
            value = ResultL.success(makeBottomSheetData(geoRecord, isDownloadOptionChecked))
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val noInternetWarning = stringResource(id = R.string.no_internet_excursions)
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
        }
    }

    LaunchedEffectWithLifecycle(flow = viewModel.locationFlow) {

    }

    /* Handle map padding and center on georecord if bottomsheet is expanded. */
    LaunchedEffect(swipeableState.currentValue, uiState, geoRecordState) {
        val mapState = (uiState as? MapReady)?.mapState ?: return@LaunchedEffect
        val geoRecord = geoRecordState.getOrNull() ?: return@LaunchedEffect

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

    ExcursionMapScreen(
        uiState = uiState,
        swipeableState = swipeableState,
        bottomSheetDataState = bottomSheetDataState,
        snackbarHostState = snackbarHostState,
        onCursorMove = { latLon, d, ele ->
            viewModel.routeLayer.setCursor(latLon, distance = d, ele = ele)
        },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExcursionMapScreen(
    uiState: UiState,
    swipeableState: SwipeableState<States>,
    bottomSheetDataState: ResultL<BottomSheetData>,
    snackbarHostState: SnackbarHostState,
    onCursorMove: (latLon: LatLon, d: Double, ele: Double) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            ExcursionMapTopAppBar(onBack = onBack)
        },
        bottomBar = {
            if (swipeableState.currentValue != States.COLLAPSED) {
                Row(
                    modifier = Modifier
                        .height(58.dp)
                        .fillMaxWidth()
                    ,
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { /*TODO*/ }) {
                        Text(stringResource(id = R.string.download))
                    }
                }
            }
        },
        snackbarHost =  {
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
                    ExcursionMap(mapState = uiState.mapState)
                    BottomSheet(swipeableState, bottomSheetDataState, onCursorMove)
                }
            }
        }
    }
}

@Composable
private fun BottomSheet(
    swipeableState: SwipeableState<States>,
    bottomSheetDataState: ResultL<BottomSheetData>,
    onCursorMove: (latLon: LatLon, d: Double, ele: Double) -> Unit = { _, _, _ -> }
) {
    CollapsibleBottomSheet(
        swipeableState = swipeableState,
//        header = {
//            Row(
//                Modifier
//                    .fillMaxWidth()
//                    .height(50.dp)
//                    .background(Color.Gray)
//            ) {
//
//            }
//        },
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
                    downloadSection(data)
                },
                onFailure = {
                    item("error") {
                        Text("Error")
                    }
                }
            )
        },
    )
}

private fun LazyListScope.statisticsSection(data: BottomSheetData) {
    item("stats") {
        TrackStats(data.geoStatistics)
    }
}

private fun LazyListScope.elevationGraphSection(
    data: BottomSheetData,
    onCursorMove: (latLon: LatLon, d: Double, ele: Double) -> Unit
) {
    if (data.elevationGraphPoints != null) {
        item("elevation-graph") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                key(data.id) {
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

private fun LazyListScope.downloadSection(data: BottomSheetData) {
    item("download-option") {
        var isChecked by remember { mutableStateOf(data.isDownloadOptionChecked) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isChecked = !isChecked }
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { isChecked = !isChecked })
            Text(text = stringResource(id = R.string.download_map_option))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExcursionMapTopAppBar(
    onBack: () -> Unit
) {
    TopAppBar(
        title = { Text("Excursions") }, // TODO: show "On foot", "Bike", etc
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
private fun ExcursionMap(modifier: Modifier = Modifier, mapState: MapState) {
    MapUI(modifier = modifier, state = mapState)
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
private const val peakedRatio = 0.4f