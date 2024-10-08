package com.peterlaurence.trekme.features.common.presentation.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.common.presentation.viewmodel.MapSelectionDialogViewModel

@Composable
fun MapSelectionDialogStateful(
    viewModel: MapSelectionDialogViewModel,
    onMapSelected: (map: Map) -> Unit,
    onDismissRequest: () -> Unit
) {
    val mapList by viewModel.mapList.collectAsState()
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val lazyListState = rememberLazyListState()

    AlertDialog(
        title = { Text(stringResource(id = R.string.choose_a_map)) },
        text = {
            MapSelectionDialog(
                mapList = mapList,
                selectedIndex = selectedIndex,
                lazyListState = lazyListState,
                onMapSelection = { selectedIndex = it }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val map = mapList.getOrNull(selectedIndex)
                    if (map != null) {
                        onMapSelected(map)
                    }
                    onDismissRequest()
                }
            ) {
                Text(stringResource(id = R.string.ok_dialog))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel_dialog_string))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun MapSelectionDialog(
    mapList: List<Map>,
    selectedIndex: Int,
    lazyListState: LazyListState,
    onMapSelection: (index: Int) -> Unit
) {
    LazyColumn(state = lazyListState) {
        itemsIndexed(mapList, key = { _, it -> it.id }) { index, map ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        if (index == selectedIndex) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else if (index % 2 == 0) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onMapSelection(index) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                val name by map.name.collectAsStateWithLifecycle()
                Text(text = name, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}