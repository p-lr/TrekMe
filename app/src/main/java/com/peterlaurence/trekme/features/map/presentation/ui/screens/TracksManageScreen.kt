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
import androidx.compose.material.DismissDirection.EndToStart
import androidx.compose.material.DismissDirection.StartToEnd
import androidx.compose.material.DismissValue.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.ConfirmDialog
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.map.presentation.ui.components.ColorPicker
import com.peterlaurence.trekme.features.map.presentation.viewmodel.TracksManageViewModel
import com.peterlaurence.trekme.util.compose.SwipeToDismiss
import com.peterlaurence.trekme.util.launchFlowCollectionWithLifecycle
import com.peterlaurence.trekme.util.parseColorL
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Composable
fun TracksManageStateful(
    viewModel: TracksManageViewModel = hiltViewModel(),
    onNavigateToMap: () -> Unit,
    onMenuClick: () -> Unit
) {
    val routes by viewModel.getRouteFlow().collectAsState()
    val excursionRefs by viewModel.getExcursionRefsFlow().collectAsState()
    var selectionId: String? by rememberSaveable { mutableStateOf(null) }

    val selectables by produceState(
        initialValue = excursionRefs.map { SelectableExcursion(it, false) } +
                routes.map { SelectableRoute(it, false) },
        key1 = routes,
        key2 = excursionRefs,
        key3 = selectionId,
        producer = {
            value = excursionRefs.map { SelectableExcursion(it, it.id == selectionId) } +
                    routes.map {
                        SelectableRoute(it, it.id == selectionId)
                    }
        }
    )

    /* Depending on whether the user has extended offer, we show different menus. */
    val hasExtendedOffer by viewModel.hasExtendedOffer.collectAsState()
    val isSelectionVisible = selectables.firstOrNull { it.isSelected }?.visible?.collectAsState()
    val topAppBarState = TopAppBarState(
        hasOverflowMenu = selectionId != null,
        hasCenterOnTrack = hasExtendedOffer && isSelectionVisible?.value == true,
        currentTrackName = selectables.firstOrNull { it.isSelected }?.name?.value ?: ""
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.also {
            viewModel.onRouteImport(uri)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMsg = stringResource(id = R.string.gpx_import_error_msg)
    val resultRecap = stringResource(id = R.string.import_result_recap)
    launchFlowCollectionWithLifecycle(viewModel.routeImportEventFlow) { result ->
        when (result) {
            GeoRecordImportResult.GeoRecordImportError -> {
                snackbarHostState.showSnackbar(errorMsg)
            }
            is GeoRecordImportResult.GeoRecordImportOk -> {
                snackbarHostState.showSnackbar(
                    resultRecap.format(
                        result.newRouteCount,
                        result.newMarkersCount
                    )
                )
            }
        }
    }

    var routeToRemoveModal by remember { mutableStateOf<Selectable?>(null) }

    TracksManageScreen(
        topAppBarState = topAppBarState,
        snackbarHostState = snackbarHostState,
        selectables = selectables,
        onMenuClick = onMenuClick,
        onRename = { newName ->
            val selectable = selectables.firstOrNull { it.isSelected }
            if (selectable != null) {
                when (selectable) {
                    is SelectableExcursion -> {
                        viewModel.onRenameExcursion(selectable.excursionRef, newName)
                    }
                    is SelectableRoute -> {
                        viewModel.onRenameRoute(selectable.route, newName)
                    }
                }
            }
        },
        onGoToRoute = {
            val selectable = selectables.firstOrNull { it.isSelected }
            if (selectable != null) {
                onNavigateToMap()
                when (selectable) {
                    is SelectableExcursion -> {
                        viewModel.centerOnExcursion(selectable.excursionRef)
                    }
                    is SelectableRoute -> {
                        viewModel.centerOnRoute(selectable.route)
                    }
                }
            }
        },
        onRouteClick = {
            selectionId = it.id
        },
        onVisibilityToggle = { id ->
            val selectable = selectables.firstOrNull { it.id == id }
            if (selectable != null) {
                when (selectable) {
                    is SelectableExcursion -> {
                        viewModel.toggleExcursionVisibility(selectable.excursionRef)
                    }
                    is SelectableRoute -> {
                        viewModel.toggleRouteVisibility(selectable.route)
                    }
                }
            }
        },
        onColorChange = { id, c ->
            val selectable = selectables.firstOrNull { it.id == id }
            if (selectable != null) {
                when (selectable) {
                    is SelectableExcursion -> {
                        viewModel.onColorChange(selectable.excursionRef, c)
                    }
                    is SelectableRoute -> {
                        viewModel.onColorChange(selectable.route, c)
                    }
                }
            }
        },
        onRemove = { selectable ->
            routeToRemoveModal = selectable
        },
        onAddNewRoute = {
            launcher.launch("*/*")
        }
    )

    routeToRemoveModal?.also { selectable ->
        ConfirmDialog(
            onConfirmPressed = {
                when (selectable) {
                    is SelectableExcursion -> viewModel.onRemoveExcursion(selectable.excursionRef)
                    is SelectableRoute -> viewModel.onRemoveRoute(selectable.route)
                }
            },
            contentText = stringResource(id = R.string.track_remove_question),
            confirmButtonText = stringResource(id = R.string.delete_dialog),
            cancelButtonText = stringResource(id = R.string.cancel_dialog_string),
            confirmColorBackground = MaterialTheme.colorScheme.error,
            onDismissRequest = { routeToRemoveModal = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TracksManageScreen(
    topAppBarState: TopAppBarState,
    snackbarHostState: SnackbarHostState,
    selectables: List<Selectable>,
    onMenuClick: () -> Unit,
    onRename: (String) -> Unit,
    onGoToRoute: () -> Unit,
    onRouteClick: (Selectable) -> Unit,
    onVisibilityToggle: (id: String) -> Unit = {},
    onColorChange: (id: String, Long) -> Unit,
    onRemove: (Selectable) -> Unit,
    onAddNewRoute: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TrackTopAppbar(
                state = topAppBarState,
                onMenuClick = onMenuClick,
                onRouteRename = onRename,
                onGoToRoute = onGoToRoute
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewRoute,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_add_24),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentDescription = stringResource(
                        id = R.string.add_track_btn_desc
                    )
                )
            }
        }
    ) { padding ->
        if (selectables.isEmpty()) {
            Text(
                stringResource(id = R.string.no_track_message),
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
            )
        } else {
            TrackList(
                Modifier.padding(padding),
                selectables = selectables,
                onRouteClick = onRouteClick,
                onVisibilityToggle = onVisibilityToggle,
                onColorChange = onColorChange,
                onRemove = onRemove
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                        contentDescription = null
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
                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                isShowingRenameModal = true
                            },
                            text = {
                                Text(stringResource(id = R.string.track_rename_action))
                            }
                        )

                        if (state.hasCenterOnTrack) {
                            DropdownMenuItem(
                                onClick = onGoToRoute,
                                text = {
                                    Text(stringResource(id = R.string.go_to_track_on_map))
                                }
                            )
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
                        containerColor = Color.Transparent,
                    )
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { isShowingRenameModal = false }
                ) {
                    Text(stringResource(id = R.string.cancel_dialog_string))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isShowingRenameModal = false
                        onRouteRename(name)
                    }
                ) {
                    Text(stringResource(id = R.string.ok_dialog))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackList(
    modifier: Modifier = Modifier,
    selectables: List<Selectable>,
    onRouteClick: (Selectable) -> Unit,
    onVisibilityToggle: (id: String) -> Unit = {},
    onColorChange: (id: String, Long) -> Unit,
    onRemove: (Selectable) -> Unit
) {
    LazyColumn(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        itemsIndexed(selectables, key = { _, it -> it.id }) { index, selectable ->
            val dismissState = rememberDismissState(
                confirmStateChange = {
                    if (it == DismissedToEnd || it == DismissedToStart) {
                        onRemove(selectable)
                    }
                    false
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
                            else -> MaterialTheme.colorScheme.error
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
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.scale(scale)
                        )
                    }
                },
            ) {
                val visible by selectable.visible.collectAsState()
                val color by selectable.color.collectAsState()
                val name by selectable.name.collectAsState()
                TrackItem(
                    Modifier.clickable {
                        onRouteClick(selectable)
                    },
                    selectable.id,
                    selectable.isSelected,
                    visible,
                    color,
                    name,
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
    id: String,
    isSelected: Boolean,
    visible: Boolean,
    color: String,
    name: String,
    index: Int,
    onVisibilityToggle: (id: String) -> Unit = {},
    onColorChange: (id: String, Long) -> Unit
) {
    var isShowingColorPicker by remember { mutableStateOf(false) }

    Row(
        modifier
            .fillMaxWidth()
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    if (index % 2 == 0) MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) else MaterialTheme.colorScheme.surface
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f)
        )

        Row {
            ColorIndicator(color, onClick = { isShowingColorPicker = true })
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(
                onClick = { onVisibilityToggle(id) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = if (visible) {
                        painterResource(id = R.drawable.ic_visibility_black_24dp)
                    } else {
                        painterResource(id = R.drawable.ic_visibility_off_black_24dp)
                    },
                    contentDescription = stringResource(id = R.string.track_visibility_btn),
                )
            }
        }

    }

    if (isShowingColorPicker) {
        ColorPicker(
            initColor = parseColorL(color),
            onColorPicked = { c ->
                onColorChange(id, c)
                isShowingColorPicker = false
            },
            onCancel = { isShowingColorPicker = false }
        )
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

private sealed interface Selectable {
    val isSelected: Boolean
    val id: String
    val name: StateFlow<String>
    val color: StateFlow<String>
    val visible: StateFlow<Boolean>
}
private data class SelectableRoute(
    val route: Route, override val isSelected: Boolean
) : Selectable {
    override val id: String
        get() = route.id
    override val name: StateFlow<String>
        get() = route.name
    override val color: StateFlow<String>
        get() = route.color
    override val visible: StateFlow<Boolean>
        get() = route.visible
}

private data class SelectableExcursion(
    val excursionRef: ExcursionRef, override val isSelected: Boolean
) : Selectable {
    override val id: String
        get() = excursionRef.id
    override val name: StateFlow<String>
        get() = excursionRef.name
    override val color: StateFlow<String>
        get() = excursionRef.color
    override val visible: StateFlow<Boolean>
        get() = excursionRef.visible
}

@Preview(showBackground = true, widthDp = 350, heightDp = 200)
@Preview(
    showBackground = true,
    widthDp = 350,
    heightDp = 200,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
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
            selectables = selectableRoutes,
            onRouteClick = {
                selectableRoutes = selectableRoutes.map { r ->
                    if (r.route.id == it.id) {
                        r.copy(isSelected = !r.isSelected)
                    } else {
                        r.copy(isSelected = false)
                    }
                }
            },
            onVisibilityToggle = {
                val route = selectableRoutes.first().route
                route.visible.update { v -> !v }
            },
            onColorChange = { _, _ -> },
            onRemove = {}
        )
    }
}
