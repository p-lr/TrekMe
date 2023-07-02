package com.peterlaurence.trekme.features.excursionsearch.presentation.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.wmts.domain.model.*
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

@Composable
fun MapSourceDataSelect(
    mapSourceDataList: List<MapSourceData>,
    currentMapSourceData: MapSourceData,
    requiresExtendedOffer: (MapSourceData) -> Boolean,
    onMapSourceDataSelected: (MapSourceData) -> Unit,
    onDismiss: () -> Unit,
    onSeeOffer: () -> Unit
) {

    val requiresOffer by remember(currentMapSourceData) {
        derivedStateOf {
            requiresExtendedOffer(currentMapSourceData)
        }
    }

    AlertDialog(
        title = { Text(text = stringResource(id = R.string.ign_select_layer_title)) },
        text = {
            MapSourceDataList(
                mapSourceDataList,
                selectedMapSource = currentMapSourceData,
                requiresExtendedOffer = requiresExtendedOffer,
                onSelection = onMapSourceDataSelected
            )
        },
        confirmButton = {
            if (requiresOffer) {
                TextButton(
                    onClick = onSeeOffer
                ) {
                    Text(stringResource(id = R.string.see_offer))
                }
            }
        },
        dismissButton = {
            if (requiresOffer) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.cancel_dialog_string))
                }
            }
        },
        onDismissRequest = onDismiss
    )
}

@Composable
private fun MapSourceDataList(
    mapSourceDataList: List<MapSourceData>,
    selectedMapSource: MapSourceData,
    requiresExtendedOffer: (MapSourceData) -> Boolean,
    onSelection: (MapSourceData) -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        mapSourceDataList.forEach { mapSourceData ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelection(mapSourceData) }
                    .padding(end = 16.dp)
            ) {
                RadioButton(
                    selected = mapSourceData == selectedMapSource, onClick = { onSelection(mapSourceData) })

                if (requiresExtendedOffer(mapSourceData)) {
                    Column {
                        Text(text = stringResource(id = mapSourceData.getNameResId()))
                        Text(
                            stringResource(id = R.string.offer_suggestion_short),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text(text = stringResource(id = mapSourceData.getNameResId()))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MapSourceDataSelectPreview() {
    TrekMeTheme {
        var selectedMapSource by remember { mutableStateOf<MapSourceData>(SwissTopoData) }

        MapSourceDataSelect(
            mapSourceDataList = listOf(
                OsmSourceData(OpenTopoMap),
                IgnSourceData(IgnClassic, emptyList()),
                SwissTopoData,
                UsgsData
            ),
            currentMapSourceData = selectedMapSource,
            requiresExtendedOffer = { it is IgnSourceData },
            onMapSourceDataSelected = { selectedMapSource = it },
            onDismiss = {},
            onSeeOffer = {}
        )
    }
}
