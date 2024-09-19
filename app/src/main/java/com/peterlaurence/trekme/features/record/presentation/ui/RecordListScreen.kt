@file:OptIn(ExperimentalMaterial3Api::class)

package com.peterlaurence.trekme.features.record.presentation.ui

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.common.domain.model.Loading
import com.peterlaurence.trekme.features.common.domain.model.RecordingsAvailable
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.MapSelectionDialogStateful
import com.peterlaurence.trekme.features.common.presentation.ui.screens.LoadingScreen as LoadingScreenCommon
import com.peterlaurence.trekme.features.common.presentation.ui.scrollbar.drawVerticalScrollbar
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.viewmodel.MapSelectionDialogViewModel
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import com.peterlaurence.trekme.features.record.presentation.ui.components.RecordItem
import com.peterlaurence.trekme.features.record.presentation.ui.components.RecordStats
import com.peterlaurence.trekme.features.record.presentation.ui.components.RecordTopAppbar
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.RecordingRenameDialog
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordListEvent
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordViewModel
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordingEvent
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordingStatisticsViewModel
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.UUID


@Composable
fun RecordListStateful(
    statViewModel: RecordingStatisticsViewModel,
    recordViewModel: RecordViewModel,
    onElevationGraphClick: (String) -> Unit,
    onGoToTrailSearchClick: () -> Unit,
    onMainMenuClick: () -> Unit,
    onNavigateToMap: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val deletionFailedMsg = stringResource(id = R.string.files_could_not_be_deleted)

    LaunchedEffectWithLifecycle(flow = statViewModel.recordingDeletionFailureFlow) {
        snackbarHostState.showSnackbar(message = deletionFailedMsg)
    }

    LifecycleResumeEffect(key1 = LocalLifecycleOwner.current) {
        recordViewModel.onResumed()
        onPauseOrDispose {  }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffectWithLifecycle(recordViewModel.events) { event ->
        when (event) {
            is RecordListEvent.RecordImport -> {
                val msg = context.getString(R.string.track_is_imported)
                val result = snackbarHostState.showSnackbar(
                    msg,
                    actionLabel = context.getString(R.string.ok_dialog)
                )
                if (result == SnackbarResult.ActionPerformed) {
                    recordViewModel.openMapForBoundingBox(
                        boundingBox = event.boundingBox,
                        recordId = event.recordId
                    )
                }
            }
            RecordListEvent.RecordRecover -> {
                val msg = context.getString(R.string.track_is_being_restored)
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
            RecordListEvent.ShowCurrentMap -> onNavigateToMap()
            RecordListEvent.NoMapContainingRecord -> {
                val msg = context.getString(R.string.track_no_map)
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uriList ->
        statViewModel.importRecordings(uriList)
    }

    val onImportFiles = {
        /* Search for all documents available via installed storage providers */
        launcher.launch("*/*")
    }

    val state by statViewModel.recordingDataFlow.collectAsState()
    val isTrackSharePending by statViewModel.isTrackSharePending.collectAsState()

    when (state) {
        Loading -> {
            LoadingScreen(onMainMenuClick)
        }
        is RecordingsAvailable -> {
            RecordListAvailableScreen(
                snackbarHostState = snackbarHostState,
                state = state as RecordingsAvailable,
                statViewModel = statViewModel,
                recordViewModel = recordViewModel,
                isTrackSharePending = isTrackSharePending,
                onMainMenuClick = onMainMenuClick,
                onGoToTrailSearchClick = onGoToTrailSearchClick,
                onElevationGraphClick = onElevationGraphClick,
                onImportFiles = onImportFiles,
                onRecordClick = { id ->
                    val recordings = state as? RecordingsAvailable ?: return@RecordListAvailableScreen
                    val bb = recordings.recordings.firstOrNull { it.id == id }?.statistics?.boundingBox ?: return@RecordListAvailableScreen
                    recordViewModel.openMapForBoundingBox(bb, id)
                }
            )
        }
    }
}

@Composable
private fun RecordListAvailableScreen(
    snackbarHostState: SnackbarHostState,
    state: RecordingsAvailable,
    statViewModel: RecordingStatisticsViewModel,
    recordViewModel: RecordViewModel,
    isTrackSharePending: Boolean,
    onMainMenuClick: () -> Unit,
    onGoToTrailSearchClick: () -> Unit,
    onElevationGraphClick: (String) -> Unit,
    onImportFiles: () -> Unit,
    onRecordClick: (String) -> Unit
) {
    val data = state.recordings
    val dataById = data.associateBy { it.id }

    var itemById by remember {
        mutableStateOf(mapOf<String, SelectableRecordingItem>())
    }

    var selectedIds by rememberSaveable {
        mutableStateOf(setOf<String>())
    }

    val isMultiSelectionMode by remember {
        derivedStateOf {
            itemById.values.any { it.isSelected }
        }
    }

    val items: List<SelectableRecordingItem> = remember(data, itemById, selectedIds) {
        data.map {
            it.toModel(selectedIds.contains(it.id))
        }.also {
            itemById = it.associateBy { item ->
                item.id
            }
        }
    }

    val selectionCount by remember(items) {
        derivedStateOf {
            items.count { it.isSelected }
        }
    }

    val lazyListState = rememberLazyListState()

    val context = LocalContext.current
    LaunchedEffectWithLifecycle(
        statViewModel.eventChannel,
        minActiveState = Lifecycle.State.RESUMED
    ) { event ->
        when (event) {
            RecordingEvent.NewRecording -> {
                lazyListState.animateScrollToItem(0)
            }
            is RecordingEvent.ShareRecordings -> {
                sendShareIntent(context, event.uris)
            }
            RecordingEvent.ShareRecordingFailure -> {
                // TODO
            }
        }
    }

    fun toggleSelected(id: String, isSelected: Boolean) {
        selectedIds = if (isSelected) {
            selectedIds.toMutableSet().apply { remove(id) }
        } else {
            selectedIds.toMutableSet().apply { add(id) }
        }
    }

    /* Don't include this lambda in the actioner, as it would cause all items to be recomposed on
     * selection change. */
    val onItemClick = { selectable: SelectableRecordingItem ->
        if (isMultiSelectionMode) {
            toggleSelected(selectable.id, selectable.isSelected)
        } else {
            onRecordClick(selectable.id)
        }
    }

    /* Don't include this lambda in the actioner, as it would cause all items to be recomposed on
     * selection change. */
    val onItemLongClick = { selectable: SelectableRecordingItem ->
        toggleSelected(selectable.id, selectable.isSelected)
    }

    var recordingRenameDialogData by rememberSaveable {
        mutableStateOf<RecordingRenameData?>(null)
    }

    var recordingForMapImport by rememberSaveable {
        mutableStateOf<Pair<String, BoundingBox>?>(null)
    }

    val scope = rememberCoroutineScope()
    val onChooseMap = { id: String ->
        val bb = state.recordings.firstOrNull { it.id == id }?.statistics?.boundingBox
        if (bb != null) {
            scope.launch {
                if (recordViewModel.hasContainingMap(bb)) {
                    recordingForMapImport = id to bb
                } else {
                    val msg = context.getString(R.string.track_no_map)
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    val actioner: Actioner = { action ->
        when (action) {
            is Action.OnEditClick -> {
                recordingRenameDialogData = RecordingRenameData(action.item.id, action.item.name.value)
            }
            is Action.OnChooseMapClick -> {
                onChooseMap(action.item.id)
            }
            is Action.OnShareClick -> {
                statViewModel.shareRecordings(listOf(action.item.id))
            }
            is Action.OnElevationGraphClick -> {
                onElevationGraphClick(action.item.id)
            }
            is Action.OnRemoveClick -> {
                val recordingData = dataById[action.item.id]
                if (recordingData != null) {
                    statViewModel.onRequestDeleteRecordings(listOf(recordingData))
                }
            }
        }
    }

    recordingRenameDialogData?.also {
        RecordingRenameDialog(
            id = it.id,
            name = it.name,
            onRename = { id, newName ->
                statViewModel.renameRecording(id, newName)
                recordingRenameDialogData = null
            },
            onDismissRequest = { recordingRenameDialogData = null }
        )
    }

    recordingForMapImport?.also { (mapId, bb) ->
        val mapSelectionDialogViewModel: MapSelectionDialogViewModel = hiltViewModel()
        mapSelectionDialogViewModel.init(bb)
        MapSelectionDialogStateful(
            viewModel = mapSelectionDialogViewModel,
            onMapSelected = { map -> recordViewModel.importRecordInMap(map.id, mapId, bb) },
            onDismissRequest = { recordingForMapImport = null }
        )
    }

    Scaffold(
        topBar = {
            RecordTopAppbar(
                selectionCount = selectionCount,
                isTrackSharePending = isTrackSharePending,
                onMainMenuClick = onMainMenuClick,
                onImportClick = onImportFiles,
                onRename = {
                    val selected = getSelected(dataById, items)
                    if (selected != null) {
                        recordingRenameDialogData = RecordingRenameData(selected.id, selected.name.value)
                    }
                },
                onChooseMap = {
                    val selected = getSelected(dataById, items)
                    if (selected != null) {
                        onChooseMap(selected.id)
                    }
                },
                onShare = {
                    val selectedList = getSelectedList(dataById, items)
                    statViewModel.shareRecordings(selectedList.map { it.id })
                },
                onShowElevationGraph = {
                    val selected = getSelected(dataById, items)
                    if (selected != null) {
                        onElevationGraphClick(selected.id)
                    }
                },
                onRemove = {
                    val selectedList = getSelectedList(dataById, items)
                    statViewModel.onRequestDeleteRecordings(selectedList)
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)

        if (items.isNotEmpty()) {
            RecordListAvailable(
                modifier = modifier,
                lazyListState = lazyListState,
                items = items,
                isMultiSelectionMode = isMultiSelectionMode,
                isTrackSharePending = isTrackSharePending,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
                actioner = actioner
            )
        } else {
            NoTrails(
                modifier = modifier,
                onImport = onImportFiles,
                onSearch = onGoToTrailSearchClick
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordListAvailable(
    modifier: Modifier = Modifier,
    items: List<SelectableRecordingItem>,
    isMultiSelectionMode: Boolean,
    lazyListState: LazyListState,
    isTrackSharePending: Boolean,
    onItemClick: (SelectableRecordingItem) -> Unit,
    onItemLongClick: (SelectableRecordingItem) -> Unit,
    actioner: Actioner,
) {
    Column(modifier) {
        LazyColumn(
            Modifier
                .drawVerticalScrollbar(lazyListState)
                .weight(1f),
            state = lazyListState
        ) {
            itemsIndexed(
                items = items,
                key = { _, it -> it.id }
            ) { index, item ->
                RecordItem(
                    modifierProvider = { Modifier.animateItemPlacement() },
                    item = item,
                    index = index,
                    isMultiSelectionMode = isMultiSelectionMode,
                    isTrackSharePending = isTrackSharePending,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                    onRename = { actioner(Action.OnEditClick(item)) },
                    onChooseMap = { actioner(Action.OnChooseMapClick(item)) },
                    onShare = { actioner(Action.OnShareClick(item)) },
                    onShowElevationGraph = { actioner(Action.OnElevationGraphClick(item)) },
                    onRemove = { actioner(Action.OnRemoveClick(item)) }
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen(onMainMenuClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.my_trails_title)) },
                navigationIcon = {
                    IconButton(onClick = onMainMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "")
                    }
                },
            )
        }
    ) { paddingValues ->
        LoadingScreenCommon(
            Modifier.padding(paddingValues),
            stringResource(id = R.string.trails_loading)
        )
    }
}

private fun sendShareIntent(context: Context, uris: List<Uri>) {
    val intentBuilder = ShareCompat.IntentBuilder(context)
        .setType("text/plain")
    uris.forEach { uri ->
        try {
            intentBuilder.addStream(uri)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
    intentBuilder.startChooser()
}

private fun RecordingData.toModel(isSelected: Boolean): SelectableRecordingItem {
    val stats = statistics?.let {
        RecordStats(
            distance = UnitFormatter.formatDistance(it.distance),
            duration = it.durationInSecond?.let { duration ->
                UnitFormatter.formatDuration(duration)
            } ?: "-",
            elevationDownStack = "-${UnitFormatter.formatElevation(it.elevationDownStack)}",
            elevationUpStack = "+${UnitFormatter.formatElevation(it.elevationUpStack)}",
            speed = it.avgSpeed?.let { speed ->
                UnitFormatter.formatSpeed(speed)
            } ?: "-"
        )
    }

    return SelectableRecordingItem(name, stats, isSelected, id)
}

private fun getSelected(
    dataById: Map<String, RecordingData>,
    items: List<SelectableRecordingItem>
): RecordingData? {
    val selectedId = items.firstOrNull { it.isSelected }?.id ?: return null
    return dataById[selectedId]
}

private fun getSelectedList(
    dataById: Map<String, RecordingData>,
    items: List<SelectableRecordingItem>
): List<RecordingData> {
    val selectedIds = items.filter { it.isSelected }
    return selectedIds.mapNotNull { dataById[it.id] }
}

private typealias Actioner = (Action) -> Unit

private sealed interface Action {
    data class OnEditClick(val item: SelectableRecordingItem): Action
    data class OnChooseMapClick(val item: SelectableRecordingItem) : Action
    data class OnShareClick(val item: SelectableRecordingItem) : Action
    data class OnElevationGraphClick(val item: SelectableRecordingItem) : Action
    data class OnRemoveClick(val item: SelectableRecordingItem) : Action
}

data class SelectableRecordingItem(
    val name: StateFlow<String>, val stats: RecordStats?,
    val isSelected: Boolean,
    val id: String
)

@Stable
@Parcelize
data class RecordingRenameData(val id: String, val name: String) : Parcelable

@Composable
private fun NoTrails(
    modifier: Modifier = Modifier,
    onImport: () -> Unit = {},
    onSearch: () -> Unit = {}
) {
    Column(modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(id = R.string.no_recording_tutorial))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            ClickableCard(
                iconId = R.drawable.ic_gpx_file,
                textId = R.string.no_recording_import_trails,
                onClick = onImport
            )
            ClickableCard(
                iconId = R.drawable.ic_earth,
                textId = R.string.no_recording_search_trail,
                onClick = onSearch
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ClickableCard(
    iconId: Int,
    textId: Int,
    onClick: () -> Unit
) {
    OutlinedCard(onClick = onClick) {
        Column(
            Modifier
                .width(120.dp)
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = iconId),
                modifier = Modifier.padding(bottom = 8.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)),
                contentDescription = null
            )
            Text(text = stringResource(id = textId), textAlign = TextAlign.Center)
        }
    }
}

@Preview(heightDp = 500, showBackground = true)
@Composable
private fun GpxRecordListPreview() {
    TrekMeTheme {
        val stats = RecordStats(
            "11.51 km",
            "+127 m",
            "-655 m",
            "2h46",
            "8.2 km/h"
        )
        RecordListAvailable(
            items = listOf(
                SelectableRecordingItem(
                    id = UUID.randomUUID().toString(), name = MutableStateFlow("Track 1"), isSelected = false, stats = stats),
                SelectableRecordingItem(id = UUID.randomUUID().toString(), name = MutableStateFlow("Track 2"), isSelected = true, stats = stats),
                SelectableRecordingItem(id = UUID.randomUUID().toString(), name = MutableStateFlow("Track 3"), isSelected = false, stats = stats)
            ),
            isMultiSelectionMode = false,
            lazyListState = LazyListState(),
            isTrackSharePending = false,
            onItemClick = {},
            onItemLongClick = {},
            actioner = {}
        )
    }
}

@Preview(heightDp = 500, showBackground = true)
@Composable
private fun GpxRecordListPreview2() {
    TrekMeTheme {
        val stats = RecordStats(
            "11.51 km",
            "+127 m",
            "-655 m",
            "2h46",
            "8.2 km/h"
        )
        RecordListAvailable(
            items = listOf(
                SelectableRecordingItem(
                    id = UUID.randomUUID().toString(),
                    name = MutableStateFlow("Track 1"),
                    isSelected = false,
                    stats = stats
                ),
                SelectableRecordingItem(
                    id = UUID.randomUUID().toString(),
                    name = MutableStateFlow("Track 2"),
                    isSelected = true,
                    stats = stats
                ),
                SelectableRecordingItem(
                    id = UUID.randomUUID().toString(),
                    name = MutableStateFlow("Track 3"),
                    isSelected = true,
                    stats = stats
                )
            ),
            isMultiSelectionMode = true,
            lazyListState = LazyListState(),
            isTrackSharePending = false,
            onItemClick = {},
            onItemLongClick = {},
            actioner = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 500)
@Composable
private fun NoTrailsPreview() {
    TrekMeTheme {
        NoTrails()
    }
}