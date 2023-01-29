package com.peterlaurence.trekme.features.record.presentation.ui.components

import android.os.Parcelable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.common.domain.model.Loading
import com.peterlaurence.trekme.features.common.domain.model.RecordingsAvailable
import com.peterlaurence.trekme.features.common.presentation.ui.scrollbar.drawVerticalScrollbar
import com.peterlaurence.trekme.features.common.presentation.ui.theme.m3.activeColor
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordingStatisticsViewModel
import com.peterlaurence.trekme.util.launchFlowCollectionWithLifecycle
import kotlinx.parcelize.Parcelize
import java.util.*

@Composable
fun GpxRecordListStateful(
    modifier: Modifier = Modifier,
    statViewModel: RecordingStatisticsViewModel,
    onImportMenuClick: () -> Unit,
    onRenameRecord: (RecordingData) -> Unit,
    onChooseMapForRecord: (RecordingData) -> Unit,
    onShareRecords: (List<RecordingData>) -> Unit,
    onElevationGraphClick: (RecordingData) -> Unit,
    onDeleteClick: (List<RecordingData>) -> Unit
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

    launchFlowCollectionWithLifecycle(statViewModel.newRecordingEventFlow) {
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

    val actioner: Actioner = { action ->
        when (action) {
            Action.OnMultiSelectionClick -> {
                isMultiSelectionMode = !isMultiSelectionMode
                if (!isMultiSelectionMode) {
                    itemById = itemById.mapValues {
                        it.value.copy(isSelected = false)
                    }
                }
            }
            Action.OnImportMenuCLick -> onImportMenuClick()
            Action.OnEditClick -> {
                val selected = getSelected(dataById, items)
                if (selected != null) {
                    onRenameRecord(selected)
                }
            }
            Action.OnChooseMapClick -> {
                val selected = getSelected(dataById, items)
                if (selected != null) {
                    onChooseMapForRecord(selected)
                }
            }
            Action.OnShareClick -> {
                val selectedList = getSelectedList(dataById, items)
                onShareRecords(selectedList)
            }
            Action.OnElevationGraphClick -> {
                val selected = getSelected(dataById, items)
                if (selected != null) {
                    onElevationGraphClick(selected)
                }
            }
            Action.OnRemoveClick -> {
                val selectedList = getSelectedList(dataById, items)
                onDeleteClick(selectedList)
            }
        }
    }

    GpxRecordList(
        modifier = modifier,
        items = items,
        isMultiSelectionMode = isMultiSelectionMode,
        lazyListState = lazyListState,
        onItemClick,
        actioner
    )
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

    Card(modifier) {
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
                    contentColor = if (isMultiSelectionMode) activeColor() else MaterialTheme.colorScheme.primary
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
                        onClick = { actioner(Action.OnImportMenuCLick) },
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
                painterResource(id = R.drawable.import_30dp),
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
    Card(modifier.fillMaxSize()) {
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
    object OnMultiSelectionClick : Action
    object OnImportMenuCLick : Action
    object OnEditClick : Action
    object OnChooseMapClick : Action
    object OnShareClick : Action
    object OnElevationGraphClick : Action
    object OnRemoveClick : Action
}

@Stable
@Parcelize
data class SelectableRecordingItem(
    val name: String, val stats: RecordStats?,
    val isSelected: Boolean,
    val id: UUID
) : Parcelable
