package com.peterlaurence.trekme.features.record.presentation.ui

import android.content.Context
import android.net.Uri
import android.os.ParcelUuid
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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import androidx.lifecycle.Lifecycle
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.features.common.domain.model.Loading
import com.peterlaurence.trekme.features.common.domain.model.RecordingsAvailable
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.MapSelectionDialogStateful
import com.peterlaurence.trekme.features.common.presentation.ui.scrollbar.drawVerticalScrollbar
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import com.peterlaurence.trekme.features.record.presentation.ui.components.RecordItem
import com.peterlaurence.trekme.features.record.presentation.ui.components.RecordStats
import com.peterlaurence.trekme.features.record.presentation.ui.components.RecordTopAppbar
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.RecordingRenameDialog
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordViewModel
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordingEvent
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordingStatisticsViewModel
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.parcelize.Parcelize
import java.util.UUID


@Composable
fun RecordListStateful(
    statViewModel: RecordingStatisticsViewModel,
    recordViewModel: RecordViewModel,
    onElevationGraphClick: (RecordingData) -> Unit,
    onGoToTrailSearchClick: () -> Unit,
    onMainMenuClick: () -> Unit,
    onRecordClick: (UUID) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val deletionFailedMsg = stringResource(id = R.string.files_could_not_be_deleted)
    val geoRecordAddMsg = stringResource(id = R.string.track_is_being_added)
    val geoRecordOutOfBoundsMsg = stringResource(id = R.string.import_result_out_of_bounds)
    val geoRecordAdErrorMsg = stringResource(id = R.string.track_add_error)
    val geoRecordRecover = stringResource(id = R.string.track_is_being_restored)

    LaunchedEffectWithLifecycle(flow = statViewModel.recordingDeletionFailureFlow) {
        snackbarHostState.showSnackbar(message = deletionFailedMsg)
    }

    LaunchedEffectWithLifecycle(recordViewModel.geoRecordImportResultFlow) { result ->
        when (result) {
            is GeoRecordImportResult.GeoRecordImportOk ->
                /* Tell the user that the track will be shortly available in the map */
                snackbarHostState.showSnackbar(geoRecordAddMsg)

            GeoRecordImportResult.GeoRecordImportError ->
                /* Tell the user that an error occurred */
                snackbarHostState.showSnackbar(geoRecordAdErrorMsg)

            GeoRecordImportResult.GeoRecordOutOfBounds ->
                /* Tell the user that the tracks is out of bounds */
                snackbarHostState.showSnackbar(geoRecordOutOfBoundsMsg)
        }
    }

    LaunchedEffectWithLifecycle(recordViewModel.geoRecordRecoverEventFlow) {
        /* Tell the user that a track is being recovered */
        snackbarHostState.showSnackbar(geoRecordRecover)
    }

    LaunchedEffectWithLifecycle(recordViewModel.excursionImportEventFlow) { success ->
        if (success) {
            snackbarHostState.showSnackbar(geoRecordAddMsg)
        } else {
            snackbarHostState.showSnackbar(geoRecordAdErrorMsg)
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
            LoadingScreen()
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
                onRecordClick = onRecordClick
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
    onElevationGraphClick: (RecordingData) -> Unit,
    onImportFiles: () -> Unit,
    onRecordClick: (UUID) -> Unit
) {
    val data = state.recordings
    val dataById = data.associateBy { it.id }

    var itemById by rememberSaveable {
        mutableStateOf(mapOf<UUID, SelectableRecordingItem>())
    }

    val isMultiSelectionMode by remember {
        derivedStateOf {
            itemById.values.any { it.isSelected }
        }
    }

    val items: List<SelectableRecordingItem> = remember(data, itemById) {
        data.map {
            val existing = itemById[it.id]
            it.toModel(existing?.isSelected ?: false)
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

    /* Don't include this lambda in the actioner, as it would cause all items to be recomposed on
     * selection change. */
    val onItemClick = { selectable: SelectableRecordingItem ->
        val copy = itemById.toMutableMap()
        if (isMultiSelectionMode) {
            copy[selectable.id] = selectable.copy(isSelected = !selectable.isSelected)
        } else {
            onRecordClick(selectable.id)
        }

        itemById = copy
    }

    /* Don't include this lambda in the actioner, as it would cause all items to be recomposed on
     * selection change. */
    val onItemLongClick = { selectable: SelectableRecordingItem ->
        val copy = itemById.toMutableMap()
        copy[selectable.id] = selectable.copy(isSelected = !selectable.isSelected)
        itemById = copy
    }

    var recordingRenameDialogData by rememberSaveable {
        mutableStateOf<RecordingRenameData?>(null)
    }

    var recordingForMapImport by rememberSaveable {
        mutableStateOf<ParcelUuid?>(null)
    }

    val actioner: Actioner = { action ->
        when (action) {
            is Action.OnEditClick -> {
                val selected = getSelected(dataById, items)
                if (selected != null) {
                    recordingRenameDialogData = RecordingRenameData(selected.id, selected.name)
                }
            }
            is Action.OnChooseMapClick -> {
                val selected = getSelected(dataById, items)
                if (selected != null) {
                    recordingForMapImport = ParcelUuid(selected.id)
                }
            }
            is Action.OnShareClick -> {
                val selectedList = getSelectedList(dataById, items)
                statViewModel.shareRecordings(selectedList.map { it.id })
            }
            is Action.OnElevationGraphClick -> {
                val selected = getSelected(dataById, items)
                if (selected != null) {
                    onElevationGraphClick(selected)
                }
            }
            is Action.OnRemoveClick -> {
                val selectedList = getSelectedList(dataById, items)
                statViewModel.onRequestDeleteRecordings(selectedList)
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

    recordingForMapImport?.also {
        MapSelectionDialogStateful(
            onMapSelected = { map -> recordViewModel.importRecordInMap(map.id, it.uuid) },
            onDismissRequest = { recordingForMapImport = null }
        )
    }

    Scaffold(
        topBar = {
            RecordTopAppbar(
                onMainMenuClick = onMainMenuClick,
                onImportClick = onImportFiles
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
                selectionCount = selectionCount,
                isTrackSharePending = isTrackSharePending,
                isMultiSelectionMode = isMultiSelectionMode,
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
    selectionCount: Int,
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
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }

        if (selectionCount > 0) {
            BottomBarButtons(selectionCount, isTrackSharePending, actioner)
        }
    }
}


@Composable
private fun BottomBarButtons(
    selectionCount: Int,
    isTrackSharePending: Boolean,
    actioner: Actioner
) {
    Row(
        Modifier.padding(horizontal = 8.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { actioner(Action.OnEditClick) },
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            enabled = selectionCount == 1
        ) {
            Icon(
                painterResource(id = R.drawable.ic_edit_black_30dp),
                contentDescription = stringResource(
                    id = R.string.recording_edit_name_desc
                )
            )
        }
        IconButton(
            onClick = { actioner(Action.OnChooseMapClick) },
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            enabled = selectionCount == 1
        ) {
            Icon(
                painterResource(id = R.drawable.import_24dp),
                contentDescription = stringResource(
                    id = R.string.recording_import_desc
                )
            )
        }
        Box {
            IconButton(
                onClick = { actioner(Action.OnShareClick) },
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                enabled = selectionCount > 0 && !isTrackSharePending
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_share_black_24dp),
                    contentDescription = stringResource(
                        id = R.string.recording_share_desc
                    )
                )
            }
            if (isTrackSharePending) {
                CircularProgressIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        IconButton(
            onClick = { actioner(Action.OnElevationGraphClick) },
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            enabled = selectionCount == 1
        ) {
            Icon(
                painterResource(id = R.drawable.elevation_graph),
                contentDescription = stringResource(
                    id = R.string.recording_show_elevations_desc
                )
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = { actioner(Action.OnRemoveClick) },
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            enabled = selectionCount > 0
        ) {
            Icon(
                painterResource(id = R.drawable.ic_delete_forever_black_30dp),
                contentDescription = stringResource(
                    id = R.string.recording_delete_desc
                )
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Scaffold { paddingValues ->
        ElevatedCard(Modifier.padding(paddingValues).fillMaxSize()) {
            Column {
                Row(
                    modifier = Modifier
                        .height(54.dp)
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(id = R.string.recordings_list_title),
                        fontSize = 17.sp
                    )
                }
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
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
    dataById: Map<UUID, RecordingData>,
    items: List<SelectableRecordingItem>
): RecordingData? {
    val selectedId = items.firstOrNull { it.isSelected }?.id ?: return null
    return dataById[selectedId]
}

private fun getSelectedList(
    dataById: Map<UUID, RecordingData>,
    items: List<SelectableRecordingItem>
): List<RecordingData> {
    val selectedIds = items.filter { it.isSelected }
    return selectedIds.mapNotNull { dataById[it.id] }
}

private typealias Actioner = (Action) -> Unit

private sealed interface Action {
    data object OnEditClick : Action
    data object OnChooseMapClick : Action
    data object OnShareClick : Action
    data object OnElevationGraphClick : Action
    data object OnRemoveClick : Action
}

@Stable
@Parcelize
data class SelectableRecordingItem(
    val name: String, val stats: RecordStats?,
    val isSelected: Boolean,
    val id: UUID
) : Parcelable

@Stable
@Parcelize
data class RecordingRenameData(val id: UUID, val name: String) : Parcelable

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
                    id = UUID.randomUUID(), name = "Track 1", isSelected = false, stats = stats),
                SelectableRecordingItem(id = UUID.randomUUID(), name = "Track 2", isSelected = true, stats = stats),
                SelectableRecordingItem(id = UUID.randomUUID(), name = "Track 3", isSelected = false, stats = stats)
            ),
            selectionCount = 0,
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
                    id = UUID.randomUUID(), name = "Track 1", isSelected = false, stats = stats),
                SelectableRecordingItem(id = UUID.randomUUID(), name = "Track 2", isSelected = true, stats = stats),
                SelectableRecordingItem(id = UUID.randomUUID(), name = "Track 3", isSelected = true, stats = stats)
            ),
            selectionCount = 2,
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