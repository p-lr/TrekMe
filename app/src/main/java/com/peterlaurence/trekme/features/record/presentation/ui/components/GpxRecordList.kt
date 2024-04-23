package com.peterlaurence.trekme.features.record.presentation.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import androidx.lifecycle.Lifecycle
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.common.domain.model.Loading
import com.peterlaurence.trekme.features.common.domain.model.RecordingsAvailable
import com.peterlaurence.trekme.features.common.presentation.ui.scrollbar.drawVerticalScrollbar
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.MapSelectionDialogStateful
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.RecordingRenameDialog
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordViewModel
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordingStatisticsViewModel
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.parcelize.Parcelize
import java.util.*

@Composable
fun GpxRecordListStateful(
    modifier: Modifier = Modifier,
    statViewModel: RecordingStatisticsViewModel,
    recordViewModel: RecordViewModel,
    onElevationGraphClick: (RecordingData) -> Unit,
    onGoToTrailSearchClick: () -> Unit
) {
    val state by statViewModel.recordingDataFlow.collectAsState()

    if (state is Loading) {
        LoadingList(modifier)
        return
    }

    val data = (state as RecordingsAvailable).recordings
    val dataById = data.associateBy { it.id }

    var isMultiSelectionMode by rememberSaveable {
        mutableStateOf(false)
    }

    var itemById by rememberSaveable {
        mutableStateOf(mapOf<UUID, SelectableRecordingItem>())
    }

    val items: List<SelectableRecordingItem> = remember(data, itemById, isMultiSelectionMode) {
        data.map {
            val existing = itemById[it.id]
            it.toModel(existing?.isSelected ?: false)
        }.also {
            itemById = it.associateBy { item -> item.id }
        }
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffectWithLifecycle(
        statViewModel.newRecordingEventFlow,
        minActiveState = Lifecycle.State.RESUMED
    ) {
        lazyListState.animateScrollToItem(0)
    }

    /* Don't include this lambda in the actioner, as it would cause all items to be recomposed on
     * selection change. */
    val onItemClick = { selectable: SelectableRecordingItem ->
        val copy = itemById.toMutableMap()
        if (isMultiSelectionMode) {
            copy[selectable.id] = selectable.copy(isSelected = !selectable.isSelected)
        } else {
            copy.forEach { (id, value) ->
                copy[id] = if (id == selectable.id) {
                    value.copy(isSelected = !selectable.isSelected)
                } else {
                    value.copy(isSelected = false)
                }
            }
        }

        itemById = copy
    }

    var recordingRenameDialogData by rememberSaveable {
        mutableStateOf<RecordingRenameData?>(null)
    }

    var recordingForMapImport by rememberSaveable {
        mutableStateOf<ParcelUuid?>(null)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uriList ->
        statViewModel.importRecordings(uriList)
    }

    val context = LocalContext.current
    val actioner: Actioner = { action ->
        when (action) {
            is Action.OnMultiSelectionClick -> {
                isMultiSelectionMode = !isMultiSelectionMode
                if (!isMultiSelectionMode) {
                    itemById = itemById.mapValues {
                        it.value.copy(isSelected = false)
                    }
                }
            }
            is Action.OnImportMenuClick -> {
                /* Search for all documents available via installed storage providers */
                launcher.launch("*/*")
            }
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
            is  Action.OnShareClick -> {
                val selectedList = getSelectedList(dataById, items)
                val intentBuilder = ShareCompat.IntentBuilder(context)
                    .setType("text/plain")
                selectedList.forEach {
                    try {
                        val uri = statViewModel.getRecordingUri(it)
                        if (uri != null) {
                            intentBuilder.addStream(uri)
                        }
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                    }
                }
                intentBuilder.startChooser()
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

    if (items.isNotEmpty()) {
        GpxRecordList(
            modifier = modifier,
            items = items,
            isMultiSelectionMode = isMultiSelectionMode,
            lazyListState = lazyListState,
            onItemClick,
            actioner
        )
    } else {
        NoTrails(
            onImport = { actioner(Action.OnImportMenuClick) },
            onSearch = onGoToTrailSearchClick
        )
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GpxRecordList(
    modifier: Modifier = Modifier,
    items: List<SelectableRecordingItem>,
    isMultiSelectionMode: Boolean,
    lazyListState: LazyListState,
    onItemClick: (SelectableRecordingItem) -> Unit,
    actioner: Actioner,
) {
    val selectionCount by remember(items) {
        derivedStateOf {
            items.count { it.isSelected }
        }
    }

    ElevatedCard(modifier) {
        Column {
            RecordingActionBar(isMultiSelectionMode, actioner)
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
                        onClick = { onItemClick(item) }
                    )
                }
            }

            if (selectionCount > 0) {
                BottomBarButtons(selectionCount, actioner)
            }
        }
    }
}

@Composable
private fun RecordingActionBar(
    isMultiSelectionMode: Boolean,
    actioner: Actioner
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(id = R.string.recordings_list_title),
            fontSize = 17.sp
        )
        Row {
            IconButton(
                onClick = { actioner(Action.OnMultiSelectionClick) },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isMultiSelectionMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                ),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.check_multiple),
                    contentDescription = stringResource(id = R.string.multi_selection_desc),
                )
            }
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.width(36.dp),
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                )
            }
            Box(
                Modifier
                    .height(24.dp)
                    .wrapContentSize(Alignment.BottomEnd, true)
            ) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(0.dp, 0.dp)
                ) {
                    DropdownMenuItem(
                        onClick = { actioner(Action.OnImportMenuClick) },
                        text = {
                            Text(stringResource(id = R.string.recordings_menu_import))
                            Spacer(Modifier.weight(1f))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarButtons(selectionCount: Int, actioner: Actioner) {
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
        IconButton(
            onClick = { actioner(Action.OnShareClick) },
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            enabled = selectionCount > 0
        ) {
            Icon(
                painterResource(id = R.drawable.ic_share_black_24dp),
                contentDescription = stringResource(
                    id = R.string.recording_share_desc
                )
            )
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
private fun LoadingList(modifier: Modifier = Modifier) {
    ElevatedCard(modifier.fillMaxSize()) {
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
    data object OnMultiSelectionClick : Action
    data object OnImportMenuClick : Action
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
    onImport: () -> Unit = {},
    onSearch: () -> Unit = {}
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
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

@Preview(heightDp = 500)
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
        GpxRecordList(
            items = listOf(
                SelectableRecordingItem(
                    id = UUID.randomUUID(), name = "Track 1", isSelected = false, stats = stats),
                SelectableRecordingItem(id = UUID.randomUUID(), name = "Track 2", isSelected = true, stats = stats),
                SelectableRecordingItem(id = UUID.randomUUID(), name = "Track 3", isSelected = false, stats = stats)
            ),
            isMultiSelectionMode = false,
            lazyListState = LazyListState(),
            onItemClick = {},
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