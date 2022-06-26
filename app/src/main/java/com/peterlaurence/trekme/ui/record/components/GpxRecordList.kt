package com.peterlaurence.trekme.ui.record.components

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.ui.Modifier
import com.peterlaurence.trekme.viewmodel.record.RecordingData
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.viewmodel.record.RecordingStatisticsViewModel
import kotlinx.parcelize.Parcelize

@Composable
fun GpxRecordListStateful(statViewModel: RecordingStatisticsViewModel) {
    val data by statViewModel.getRecordingData().observeAsState(listOf())

    var isMultiSelectionMode by rememberSaveable {
        mutableStateOf(false)
    }
    var dataToModel by rememberSaveable {
        mutableStateOf(mutableMapOf<String, SelectableRecordingData>())
    }

    val model = remember(data, dataToModel, isMultiSelectionMode) {
        data.map {
            val existing = dataToModel[it.gpxFile.path]
            it.toModel(existing?.isSelected ?: false).also {  selectable ->
                dataToModel[selectable.id] = selectable
            }
        }
    }

    GpxRecordList(
        modifier = Modifier,
        data = model,
        isMultiSelectionMode = isMultiSelectionMode,
        onMultiSelectionClick = {
            isMultiSelectionMode = !isMultiSelectionMode
            if (!isMultiSelectionMode) {
                dataToModel = dataToModel.mapValues {
                    it.value.copy(isSelected = false)
                }.toMutableMap()
            }
        },
        onClick = { selectable ->
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

            dataToModel  = copy
        }
    )
}


@Composable
private fun GpxRecordList(
    modifier: Modifier,
    data: List<SelectableRecordingData>,
    isMultiSelectionMode: Boolean,
    onMultiSelectionClick: () -> Unit,
    onClick: (item: SelectableRecordingData) -> Unit
) {
    Card(modifier) {
        Column {
            Button(onClick = onMultiSelectionClick) {
                Text("Multi selection")
            }
            println("xxxxx recomposing list")
            LazyColumn {
                itemsIndexed(
                    items = data,
                    key = { _, it -> it.id }
                ) { index, record ->
                    RecordItem(
                        name = record.name,
                        stats = record.stats,
                        isSelected = record.isSelected,
                        isMultiSelectionMode = isMultiSelectionMode,
                        index = index,
                        onClick = { onClick(record) }
                    )
                }
            }
        }
    }
}

private fun RecordingData.toModel(isSelected: Boolean, ): SelectableRecordingData {
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

@Parcelize
private data class SelectableRecordingData(
    val name: String, val stats: RecordStats?,
    val isSelected: Boolean,
    val id: String
) : Parcelable