package com.peterlaurence.trekme.ui.record.components

import android.os.Parcelable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.common.presentation.ui.scrollbar.drawVerticalScrollbar
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentColor
import com.peterlaurence.trekme.features.common.presentation.ui.theme.surfaceBackground
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textButtonColor
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor
import com.peterlaurence.trekme.viewmodel.record.RecordingData
import com.peterlaurence.trekme.viewmodel.record.RecordingStatisticsViewModel
import kotlinx.parcelize.Parcelize

@Composable
fun GpxRecordListStateful(
    statViewModel: RecordingStatisticsViewModel,
    onImportMenuClick: () -> Unit,
    onRenameRecord: (RecordingData) -> Unit,
    onChooseMapForRecord: (RecordingData) -> Unit,
    onShareRecords: (List<RecordingData>) -> Unit,
    onElevationGraphClick: (RecordingData) -> Unit,
    onDeleteClick: (List<RecordingData>) -> Unit
) {
    val data by statViewModel.getRecordingData().observeAsState(listOf())
    val dataById = data.associateBy { it.gpxFile.path }

    var isMultiSelectionMode by rememberSaveable {
        mutableStateOf(false)
    }
    var dataToModel by rememberSaveable {
        mutableStateOf(mutableMapOf<String, SelectableRecordingData>())
    }

    val model = remember(data, dataToModel, isMultiSelectionMode) {
        data.map {
            val existing = dataToModel[it.gpxFile.path]
            it.toModel(existing?.isSelected ?: false).also { selectable ->
                dataToModel[selectable.id] = selectable
            }
        }
    }

    GpxRecordList(
        data = model,
        isMultiSelectionMode = isMultiSelectionMode
    ) { action ->
        when(action) {
            Action.OnMultiSelectionClick -> {
                isMultiSelectionMode = !isMultiSelectionMode
                if (!isMultiSelectionMode) {
                    dataToModel = dataToModel.mapValues {
                        it.value.copy(isSelected = false)
                    }.toMutableMap()
                }
            }
            Action.OnImportMenuCLick -> onImportMenuClick()
            is Action.OnRecordClick -> {
                val selectable = action.selectable
                val copy = dataToModel.toMutableMap()
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

                dataToModel = copy
            }
            Action.OnEditClick -> {
                val selected = getSelected(dataById, model) ?: return@GpxRecordList
                onRenameRecord(selected)
            }
            Action.OnChooseMapClick -> {
                val selected = getSelected(dataById, model) ?: return@GpxRecordList
                onChooseMapForRecord(selected)
            }
            Action.OnShareClick -> {
                val selectedList = getSelectedList(dataById, model)
                onShareRecords(selectedList)
            }
            Action.OnElevationGraphClick -> {
                val selected = getSelected(dataById, model) ?: return@GpxRecordList
                onElevationGraphClick(selected)
            }
            Action.OnRemoveClick -> {
                val selectedList = getSelectedList(dataById, model)
                onDeleteClick(selectedList)
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GpxRecordList(
    modifier: Modifier = Modifier,
    data: List<SelectableRecordingData>,
    isMultiSelectionMode: Boolean,
    actioner: Actioner,
) {
    val selectionCount by remember(data) {
        derivedStateOf {
            data.count { it.isSelected }
        }
    }

    val state = rememberLazyListState()

    Card(modifier) {
        Column {
            RecordingActionBar(isMultiSelectionMode, actioner)
            LazyColumn(
                Modifier
                    .drawVerticalScrollbar(state)
                    .weight(1f),
                state = state
            ) {
                itemsIndexed(
                    items = data,
                    key = { _, it -> it.id }
                ) { index, record ->
                    RecordItem(
                        Modifier.animateItemPlacement(),
                        name = record.name,
                        stats = record.stats,
                        isSelected = record.isSelected,
                        isMultiSelectionMode = isMultiSelectionMode,
                        index = index,
                        onClick = { actioner(Action.OnRecordClick(record)) }
                    )
                }
            }

            BottomBarButtons(selectionCount, actioner)
        }
    }
}

@Composable
private fun RecordingActionBar(
    isMultiSelectionMode: Boolean,
    actioner: Actioner
) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                stringResource(id = R.string.recordings_list_title),
                color = textColor(),
                fontSize = 17.sp
            )
        },
        actions = {
            IconButton(onClick = { actioner(Action.OnMultiSelectionClick) }) {
                Icon(
                    painterResource(id = R.drawable.check_multiple),
                    contentDescription = stringResource(id = R.string.multi_selection_desc),
                    tint = if (isMultiSelectionMode) accentColor() else textButtonColor()
                )
            }
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.width(36.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = textButtonColor()
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
                        onClick = { actioner(Action.OnImportMenuCLick) }
                    ) {
                        Text(stringResource(id = R.string.recordings_menu_import))
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        },
        backgroundColor = surfaceBackground()
    )
}

@Composable
private fun BottomBarButtons(selectionCount: Int, actioner: Actioner) {
    Row(
        Modifier.padding(horizontal = 8.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { actioner(Action.OnEditClick) },
            enabled = selectionCount == 1
        ) {
            Image(
                painterResource(id = R.drawable.ic_edit_black_30dp),
                colorFilter = ColorFilter.tint(
                    if (selectionCount == 1) accentColor() else textButtonColor()
                ),
                contentDescription = stringResource(
                    id = R.string.recording_edit_name_desc
                )
            )
        }
        IconButton(
            onClick = { actioner(Action.OnChooseMapClick) },
            enabled = selectionCount == 1
        ) {
            Image(
                painterResource(id = R.drawable.import_30dp),
                colorFilter = ColorFilter.tint(
                    if (selectionCount == 1) accentColor() else textButtonColor()
                ),
                contentDescription = stringResource(
                    id = R.string.recording_import_desc
                )
            )
        }
        IconButton(
            onClick = { actioner(Action.OnShareClick) },
            enabled = selectionCount > 0
        ) {
            Image(
                painterResource(id = R.drawable.ic_share_black_24dp),
                colorFilter = ColorFilter.tint(
                    if (selectionCount > 0) accentColor() else textButtonColor()
                ),
                contentDescription = stringResource(
                    id = R.string.recording_share_desc
                )
            )
        }
        IconButton(
            onClick = { actioner(Action.OnElevationGraphClick) },
            enabled = selectionCount == 1
        ) {
            Image(
                painterResource(id = R.drawable.elevation_graph),
                colorFilter = ColorFilter.tint(
                    if (selectionCount == 1) accentColor() else textButtonColor()
                ),
                contentDescription = stringResource(
                    id = R.string.recording_show_elevations_desc
                )
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = { actioner(Action.OnRemoveClick) },
            enabled = selectionCount > 0
        ) {
            Image(
                painterResource(id = R.drawable.ic_delete_forever_black_30dp),
                colorFilter = ColorFilter.tint(
                    if (selectionCount > 0) colorResource(id = R.color.colorAccentRed) else textButtonColor()
                ),
                contentDescription = stringResource(
                    id = R.string.recording_delete_desc
                )
            )
        }
    }
}

private fun RecordingData.toModel(isSelected: Boolean): SelectableRecordingData {
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
    val id = gpxFile.path

    return SelectableRecordingData(name, stats, isSelected, id)
}

private fun getSelected(dataById: Map<String, RecordingData>, model: List<SelectableRecordingData>): RecordingData? {
    val selectedId = model.firstOrNull { it.isSelected }?.id ?: return null
    return dataById[selectedId]
}

private fun getSelectedList(dataById: Map<String, RecordingData>, model: List<SelectableRecordingData>): List<RecordingData> {
    val selectedIds = model.filter { it.isSelected }
    return selectedIds.mapNotNull { dataById[it.id] }
}

private typealias Actioner = (Action) -> Unit

private sealed interface Action {
    object OnMultiSelectionClick : Action
    object OnImportMenuCLick : Action
    data class OnRecordClick(val selectable: SelectableRecordingData) : Action
    object OnEditClick : Action
    object OnChooseMapClick : Action
    object OnShareClick : Action
    object OnElevationGraphClick : Action
    object OnRemoveClick : Action
}

@Stable
@Parcelize
private data class SelectableRecordingData(
    val name: String, @Stable val stats: RecordStats?,
    val isSelected: Boolean,
    val id: String
) : Parcelable