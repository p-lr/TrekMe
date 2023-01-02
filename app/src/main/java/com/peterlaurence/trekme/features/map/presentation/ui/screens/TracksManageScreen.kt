package com.peterlaurence.trekme.features.map.presentation.ui.screens

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.DismissDirection.EndToStart
import androidx.compose.material.DismissDirection.StartToEnd
import androidx.compose.material.DismissValue.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.features.common.presentation.ui.theme.*
import com.peterlaurence.trekme.features.map.presentation.ui.components.ColorPicker
import com.peterlaurence.trekme.features.map.presentation.viewmodel.TracksManageViewModel
import com.peterlaurence.trekme.util.launchFlowCollectionWithLifecycle
import com.peterlaurence.trekme.util.parseColorL
import kotlinx.coroutines.flow.update

@Composable
fun TracksManageStateful(
    viewModel: TracksManageViewModel,
    onGoToRoute: (Route) -> Unit,
    onMenuClick: () -> Unit
) {
    val routes by viewModel.getRouteFlow().collectAsState()
    var selectionId: String? by rememberSaveable { mutableStateOf(null) }

    val selectableRoutes by produceState(
        initialValue = routes.map { SelectableRoute(it, false) },
        key1 = routes,
        key2 = selectionId,
        producer = {
            value = routes.map {
                SelectableRoute(it, it.id == selectionId)
            }
        }
    )

    /* Depending on whether the user has extended offer, we show different menus. */
    val hasExtendedOffer by viewModel.hasExtendedOffer.collectAsState()
    val topAppBarState = TopAppBarState(
        hasOverflowMenu = selectionId != null,
        hasCenterOnTrack = hasExtendedOffer,
        currentTrackName = selectableRoutes.firstOrNull { it.isSelected }?.route?.name?.value ?: ""
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.also {
            viewModel.onRouteImport(uri)
        }
    }

    val scaffoldState = rememberScaffoldState()
    val errorMsg = stringResource(id = R.string.gpx_import_error_msg)
    val resultRecap = stringResource(id = R.string.import_result_recap)
    launchFlowCollectionWithLifecycle(viewModel.routeImportEventFlow) { result ->
        when (result) {
            GeoRecordImportResult.GeoRecordImportError -> {
                scaffoldState.snackbarHostState.showSnackbar(errorMsg)
            }
            is GeoRecordImportResult.GeoRecordImportOk -> {
                scaffoldState.snackbarHostState.showSnackbar(
                    resultRecap.format(
                        result.newRouteCount,
                        result.newMarkersCount
                    )
                )
            }
        }
    }

    TracksManageScreen(
        topAppBarState = topAppBarState,
        scaffoldState = scaffoldState,
        selectableRoutes = selectableRoutes,
        onMenuClick = onMenuClick,
        onRouteRename = { newName ->
            val route = selectableRoutes.firstOrNull { it.isSelected }?.route
            if (route != null) {
                viewModel.onRenameRoute(route, newName)
            }
        },
        onGoToRoute = {
            val route = selectableRoutes.firstOrNull { it.isSelected }?.route
            if (route != null) {
                onGoToRoute(route)
            }
        },
        onRouteClick = {
            selectionId = it.route.id
        },
        onVisibilityToggle = { selectableRoute ->
            viewModel.toggleRouteVisibility(selectableRoute.route)
        },
        onColorChange = { selectableRoute, c ->
            viewModel.onColorChange(selectableRoute.route, c)
        },
        onRemove = { selectableRoute ->
            viewModel.onRemoveRoute(selectableRoute.route)
        },
        onAddNewRoute = {
            launcher.launch("*/*")
        }
    )
}

@Composable
private fun TracksManageScreen(
    topAppBarState: TopAppBarState,
    scaffoldState: ScaffoldState,
    selectableRoutes: List<SelectableRoute>,
    onMenuClick: () -> Unit,
    onRouteRename: (String) -> Unit,
    onGoToRoute: () -> Unit,
    onRouteClick: (SelectableRoute) -> Unit,
    onVisibilityToggle: (SelectableRoute) -> Unit = {},
    onColorChange: (SelectableRoute, Long) -> Unit,
    onRemove: (SelectableRoute) -> Unit,
    onAddNewRoute: () -> Unit
) {
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TrackTopAppbar(
                state = topAppBarState,
                onMenuClick = onMenuClick,
                onRouteRename = onRouteRename,
                onGoToRoute = onGoToRoute
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNewRoute) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_add_24),
                    tint = Color.White,
                    contentDescription = stringResource(
                        id = R.string.add_track_btn_desc
                    )
                )
            }
        }
    ) { padding ->
        if (selectableRoutes.isEmpty()) {
            Text(stringResource(id = R.string.no_track_message), Modifier.padding(16.dp), color = textColor())
        } else {
            TrackList(
                Modifier.padding(padding),
                selectableRoutes = selectableRoutes,
                onRouteClick = onRouteClick,
                onVisibilityToggle = onVisibilityToggle,
                onColorChange = onColorChange,
                onRemove = onRemove
            )
        }
    }
}

@Composable
private fun TrackTopAppbar(
    state: TopAppBarState,
    onMenuClick: () -> Unit,
    onRouteRename: (String) -> Unit,
    onGoToRoute: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var isShowingRenameModal by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(text = stringResource(id = R.string.tracks_manage_frgmt_title)) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "")
            }
        },
        actions = {
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
                        DropdownMenuItem(onClick = {
                            expanded = false
                            isShowingRenameModal = true
                        }) {
                            Text(stringResource(id = R.string.track_rename_action))
                        }

                        if (state.hasCenterOnTrack) {
                            DropdownMenuItem(onClick = onGoToRoute) {
                                Text(stringResource(id = R.string.go_to_track_on_map))
                            }
                        }
                    }
                }
            }
        }
    )

    if (isShowingRenameModal) {
        var name by remember { mutableStateOf(state.currentTrackName) }
        AlertDialog(
            onDismissRequest = { isShowingRenameModal = false },
            title = {
                Text(stringResource(id = R.string.track_name_change))
            },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        textColor = textColor()
                    )
                )
            },
            buttons = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = accentColor()),
                        onClick = { isShowingRenameModal = false }
                    ) {
                        Text(stringResource(id = R.string.cancel_dialog_string))
                    }
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = accentColor()),
                        onClick = {
                            isShowingRenameModal = false
                            onRouteRename(name)
                        }
                    ) {
                        Text(stringResource(id = R.string.ok_dialog))
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackList(
    modifier: Modifier = Modifier,
    selectableRoutes: List<SelectableRoute>,
    onRouteClick: (SelectableRoute) -> Unit,
    onVisibilityToggle: (SelectableRoute) -> Unit = {},
    onColorChange: (SelectableRoute, Long) -> Unit,
    onRemove: (SelectableRoute) -> Unit
) {
    LazyColumn(
        modifier
            .fillMaxSize()
            .background(backgroundColor())
    ) {
        itemsIndexed(selectableRoutes, key = { _, it -> it.route.id }) { index, selectableRoute ->
            val dismissState = rememberDismissState(
                confirmStateChange = {
                    if (it == DismissedToEnd || it == DismissedToStart) {
                        onRemove(selectableRoute)
                    }
                    true
                }
            )
            SwipeToDismiss(
                state = dismissState,
                modifier = Modifier.animateItemPlacement(),
                directions = setOf(StartToEnd, EndToStart),
                background = {
                    val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                    val color by animateColorAsState(
                        when (dismissState.targetValue) {
                            Default -> Color.LightGray
                            else -> colorResource(id = R.color.colorAccentRed)
                        }
                    )
                    val alignment = when (direction) {
                        StartToEnd -> Alignment.CenterStart
                        EndToStart -> Alignment.CenterEnd
                    }
                    val icon = Icons.Default.Delete
                    val scale by animateFloatAsState(
                        if (dismissState.targetValue == Default) 0.75f else 1f
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(color)
                            .padding(horizontal = 20.dp),
                        contentAlignment = alignment
                    ) {
                        Icon(
                            icon,
                            contentDescription = stringResource(id = R.string.delete_dialog),
                            modifier = Modifier.scale(scale)
                        )
                    }
                },
            ) {
                TrackItem(
                    Modifier.clickable {
                        onRouteClick(selectableRoute)
                    },
                    selectableRoute,
                    index,
                    onVisibilityToggle = onVisibilityToggle,
                    onColorChange = onColorChange
                )
            }
        }
    }
}

@Composable
private fun TrackItem(
    modifier: Modifier = Modifier,
    selectableRoute: SelectableRoute,
    index: Int,
    onVisibilityToggle: (SelectableRoute) -> Unit = {},
    onColorChange: (SelectableRoute, Long) -> Unit
) {
    val visible by selectableRoute.route.visible.collectAsState()
    val color by selectableRoute.route.color.collectAsState()
    val name by selectableRoute.route.name.collectAsState()

    var isShowingColorPicker by remember { mutableStateOf(false) }

    Row(
        modifier
            .fillMaxWidth()
            .background(
                if (selectableRoute.isSelected) {
                    if (isSystemInDarkTheme()) Color(0xff3b5072) else Color(0xffc1d8ff)
                } else {
                    if (index % 2 == 1) surfaceBackground() else {
                        if (isSystemInDarkTheme()) Color(0xff3c3c3c) else Color(0xffeaeaea)
                    }
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = textColor(),
            modifier = Modifier.weight(1f)
        )

        Row {
            ColorIndicator(color, onClick = { isShowingColorPicker = true })
            Spacer(modifier = Modifier.width(10.dp))
            Image(
                painter = if (visible) {
                    painterResource(id = R.drawable.ic_visibility_black_24dp)
                } else {
                    painterResource(id = R.drawable.ic_visibility_off_black_24dp)
                },
                modifier = Modifier.clickable { onVisibilityToggle(selectableRoute) },
                contentDescription = stringResource(id = R.string.track_visibility_btn),
                colorFilter = ColorFilter.tint(textColor())
            )
        }

    }

    if (isShowingColorPicker) {
        Dialog(onDismissRequest = { isShowingColorPicker = false }) {
            ColorPicker(
                initColor = parseColorL(color),
                onColorPicked = { c ->
                    onColorChange(selectableRoute, c)
                    isShowingColorPicker = false
                },
                onCancel = { isShowingColorPicker = false }
            )
        }
    }
}

@Composable
private fun ColorIndicator(color: String, onClick: () -> Unit = {}) {
    val colorContent = remember(color) {
        Color(parseColorL(color))
    }
    val background = if (isSystemInDarkTheme()) Color(0xffa9b7c6) else Color.White
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .clickable(onClick = onClick)
    ) {
        val r = 10.dp.toPx()
        val r2 = 12.dp.toPx()
        drawCircle(background, r2)
        drawCircle(colorContent, r)
    }
}

private data class TopAppBarState(
    val hasOverflowMenu: Boolean,
    val hasCenterOnTrack: Boolean,
    val currentTrackName: String
)

private data class SelectableRoute(val route: Route, val isSelected: Boolean)

@Preview(showBackground = true, widthDp = 350)
@Preview(showBackground = true, widthDp = 350, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrackListPreview() {
    TrekMeTheme {
        var selectableRoutes by remember {
            mutableStateOf(
                listOf(
                    SelectableRoute(
                        Route(
                            id = "id#1",
                            "A route with a really long name and description"
                        ), false
                    ),
                    SelectableRoute(Route(id = "id#2", "A route2"), true),
                    SelectableRoute(Route(id = "id#3", "A route3"), false),
                )
            )
        }

        TrackList(
            selectableRoutes = selectableRoutes,
            onRouteClick = {
                selectableRoutes = selectableRoutes.map { r ->
                    if (r.route.id == it.route.id) {
                        r.copy(isSelected = !r.isSelected)
                    } else {
                        r.copy(isSelected = false)
                    }
                }
            },
            onVisibilityToggle = {
                it.route.visible.update { v -> !v }
            },
            onColorChange = { _, _ -> },
            onRemove = {}
        )
    }
}
