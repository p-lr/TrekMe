package com.peterlaurence.trekme.features.excursionsearch.presentation.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.wmts.domain.model.*
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

@Composable
fun MapSourceDataSelect(
    mapSourceDataList: List<MapSourceData>,
    initialSelectedIndex: Int,
    hasExtendedOffer: Boolean,
    onMapSourceDataSelected: (MapSourceData) -> Unit,
    onDismiss: () -> Unit
) {

    var selectedIndex by rememberSaveable { mutableStateOf(initialSelectedIndex) }

    AlertDialog(
        title = { Text(text = stringResource(id = R.string.ign_select_layer_title)) },
        text = {
            MapSourceDataList(
                mapSourceDataList,
                selectedIndex,
                onSelection = { selectedIndex = it }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = mapSourceDataList.elementAtOrNull(selectedIndex)
                    if (selected != null) {
                        onMapSourceDataSelected(selected)
                    }
                }
            ) {
                Text(stringResource(id = R.string.ok_dialog))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel_dialog_string))
            }
        },
        onDismissRequest = onDismiss
    )
}

@Composable
private fun MapSourceDataList(
    mapSourceDataList: List<MapSourceData>,
    selectedIndex: Int,
    onSelection: (index: Int) -> Unit
) {
    val values = mapSourceDataList.map {
        stringResource(id = it.getNameResId())
    }

    val groups = mapSourceDataList.groupBy {
        it::class.java.simpleName
    }

    Column {
        values.forEachIndexed { index, value ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onSelection(index) }
                    .padding(end = 16.dp)
            ) {
                RadioButton(
                    selected = index == selectedIndex, onClick = { onSelection(index) })
                Text(text = value)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PrimaryLayerDialogPreview() {
    TrekMeTheme {
        var indexSelected by remember { mutableStateOf(0) }
        MapSourceDataList(
            listOf(
                IgnSourceData(PlanIgnV2, emptyList()),
                IgnSourceData(IgnClassic, emptyList()),
                SwissTopoData,
                UsgsData
            ),
            indexSelected,
            onSelection = { indexSelected = it }
        )
    }
}