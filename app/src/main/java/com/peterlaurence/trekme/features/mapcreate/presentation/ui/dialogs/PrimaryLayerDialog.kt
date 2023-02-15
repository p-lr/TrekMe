package com.peterlaurence.trekme.features.mapcreate.presentation.ui.dialogs

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
fun PrimaryLayerDialogStateful(
    layerIds: List<String>,
    initialActiveLayerId: String,
    onLayerSelected: (id: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIndex by rememberSaveable { mutableStateOf(layerIds.indexOf(initialActiveLayerId)) }

    AlertDialog(
        title = { Text(text = stringResource(id = R.string.ign_select_layer_title)) },
        text = {
            PrimaryLayerDialog(
                values = layerIds.mapNotNull {
                    layerIdToResId[it]?.let { resId ->
                        stringResource(id = resId)
                    }
                },
                selectedIndex,
                onSelection = { selectedIndex = it }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onLayerSelected(layerIds.elementAt(selectedIndex)) }
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
private fun PrimaryLayerDialog(
    values: List<String>,
    selectedIndex: Int,
    onSelection: (index: Int) -> Unit
) {
    Column {
        values.forEachIndexed { index, value ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onSelection(index) }
                    .padding(end = 16.dp)
            ) {
                RadioButton(selected = index == selectedIndex, onClick = { onSelection(index) })
                Text(text = value)
            }
        }
    }
}

private val layerIdToResId = mapOf(
    ignPlanv2 to R.string.layer_ign_plan_v2,
    ignClassic to R.string.layer_ign_classic,
    ignSatellite to R.string.layer_ign_satellite,
    osmTopo to R.string.layer_osm_topo,
    osmStreet to R.string.layer_osm_street,
    openTopoMap to R.string.layer_osm_opentopo
)

@Preview(showBackground = true)
@Composable
fun PrimaryLayerDialogPreview() {
    TrekMeTheme {
        var indexSelected by remember { mutableStateOf(0) }
        PrimaryLayerDialog(
            listOf("Layer 1", "Layer 2", "Layer 3"),
            indexSelected,
            onSelection = { indexSelected = it }
        )
    }
}