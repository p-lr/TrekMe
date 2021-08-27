package com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.geocoding.GeoPlace
import com.peterlaurence.trekme.viewmodel.mapcreate.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.DefaultCanvas
import ovh.plrapps.mapcompose.api.fullSize
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.abs
import kotlin.math.min

@Composable
fun GoogleMapWmtsUI(
    modifier: Modifier,
    wmtsState: WmtsState,
    onValidateArea: () -> Unit
) {
    when (wmtsState) {
        is MapReady -> {
            MapUI(state = wmtsState.mapState)
        }
        is Loading -> {
            LoadingScreen()
        }
        is AreaSelection -> {
            AreaSelectionScreen(modifier, wmtsState, onValidateArea)
        }
        is WmtsError -> {
            ErrorScreen(wmtsState)
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
        Image(
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
private fun ErrorScreen(state: WmtsError) {
    val message = when (state) {
        WmtsError.IGN_OUTAGE -> stringResource(id = R.string.mapcreate_warning_ign)
        WmtsError.VPS_FAIL -> stringResource(id = R.string.mapreate_warning_vps)
        WmtsError.PROVIDER_OUTAGE -> stringResource(id = R.string.mapcreate_warning_others)
    }
    Error(message)
}

@Composable
private fun Error(message: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_emoji_disappointed_face_1f61e),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .padding(16.dp)
        )
        Text(text = message)
    }
}

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            Modifier
                .align(Alignment.Center)
                .width(100.dp)
        )
    }
}

@Composable
fun SearchAppBar(
    state: TopBarState,
    onSearchClick: () -> Unit,
    onCloseSearch: () -> Unit,
    onMenuClick: () -> Unit,
    onQueryTextSubmit: (String) -> Unit,
    onLayerSelection: () -> Unit,
    onZoomOnPosition: () -> Unit,
    onShowLayerOverlay: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
        },
        navigationIcon = if (state is SearchMode) {
            {
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "")
                }
            }
        } else {
            {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, contentDescription = "")
                }
            }
        },
        actions = {
            when (state) {
                is Empty -> {
                }
                is Collapsed -> {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_search_24),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    if (state.hasLayers) {
                        IconButton(onClick = onLayerSelection) {
                            Icon(
                                painter = painterResource(id = R.drawable.layer),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(onClick = onZoomOnPosition) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gps_fixed_24dp),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    if (state.hasOverflowMenu) {
                        IconButton(
                            onClick = { expanded = true },
                            modifier = Modifier.width(36.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Box(
                            Modifier
                                .height(24.dp)
                                .wrapContentSize(Alignment.BottomEnd, true)
                        ) {
                            DropdownMenu(
                                modifier = Modifier.wrapContentSize(Alignment.TopEnd),
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                offset = DpOffset(0.dp, 0.dp)
                            ) {
                                DropdownMenuItem(onClick = onShowLayerOverlay) {
                                    Text(stringResource(id = R.string.mapcreate_overlay_layers))
                                }
                            }
                        }
                    }
                }
                is SearchMode -> {
                    SearchView(state.textValueState, onQueryTextSubmit)
                }
            }
        }
    )
}

@Composable
fun SearchView(state: MutableState<TextFieldValue>, onTextChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }

    TextField(
        value = state.value,
        onValueChange = { value ->
            state.value = value
            onTextChange(value.text)
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
        placeholder = {
            Text(
                text = "Search…",
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.alpha(0.5f)
            )
        },
        trailingIcon = {
            if (state.value != TextFieldValue("")) {
                IconButton(
                    onClick = {
                        state.value =
                            TextFieldValue("") // Remove text from TextField when you press the 'X' icon
                    }
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "",
                        modifier = Modifier
                            .padding(15.dp)
                            .size(24.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
        colors = TextFieldDefaults.textFieldColors(
            textColor = Color.White,
            cursorColor = Color.White,
            leadingIconColor = Color.White,
            trailingIconColor = Color.White,
            backgroundColor = colorResource(id = R.color.colorPrimary),
//            focusedIndicatorColor = Color.Transparent,
//            unfocusedIndicatorColor = Color.Transparent,
//            disabledIndicatorColor = Color.Transparent
        )

    )

    DisposableEffect(Unit) {
        focusRequester.requestFocus()
        onDispose { }
    }
}

@Composable
fun GoogleMapWmts(
    viewModel: GoogleMapWmtsViewModel, onLayerSelection: () -> Unit, onShowLayerOverlay: () -> Unit,
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val topBarState by viewModel.topBarState.collectAsState()

    val events = viewModel.eventListState.toList()

    MyScaffold(
        events,
        topBarState,
        uiState,
        viewModel::acknowledgeError,
        viewModel::toggleArea,
        viewModel::onValidateArea,
        onMenuClick,
        viewModel::onSearchClick,
        viewModel::onCloseSearch,
        viewModel::onQueryTextSubmit,
        viewModel::moveToPlace,
        onLayerSelection,
        viewModel::zoomOnPosition,
        onShowLayerOverlay
    )
}

@Composable
fun MyScaffold(
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
    onShowLayerOverlay: () -> Unit
) {
    val scaffoldState: ScaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()


    if (events.isNotEmpty()) {
        val ok = stringResource(id = R.string.ok)
        val message = when (events.first()) {
            WmtsEvent.CURRENT_LOCATION_OUT_OF_BOUNDS -> stringResource(id = R.string.mapcreate_out_of_bounds)
            WmtsEvent.PLACE_OUT_OF_BOUNDS -> stringResource(id = R.string.place_outside_of_covered_area)
        }

        SideEffect {
            scope.launch {
                /* Dismiss the currently showing snackbar, if any */
                scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()

                scaffoldState.snackbarHostState
                    .showSnackbar(message, actionLabel = ok)
            }
            onAckError()
        }
    }

    Scaffold(
        Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        topBar = {
            SearchAppBar(
                topBarState,
                onSearchClick,
                onCloseSearch,
                onMenuClick,
                onQueryTextSubmit,
                onLayerSelection,
                onZoomOnPosition,
                onShowLayerOverlay
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
    ) {
        when (uiState) {
            is GeoplaceList -> {
                LazyColumn {
                    items(uiState.geoPlaceList) { place ->
                        Column(Modifier.clickable { onGeoPlaceSelection(place) }) {
                            Text(
                                text = place.name,
                                Modifier.padding(start = 24.dp, top = 8.dp),
                                fontSize = 17.sp
                            )
                            Text(text = place.locality, Modifier.padding(start = 24.dp, top = 4.dp))
                            Divider(Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
            is Wmts -> {
                GoogleMapWmtsUI(
                    Modifier.fillMaxSize(),
                    uiState.wmtsState,
                    onValidateArea,
                )
            }
        }
    }
}