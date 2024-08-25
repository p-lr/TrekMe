package com.peterlaurence.trekme.features.record.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTopAppbar(
    onMainMenuClick: () -> Unit,
    onImportClick: () -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.my_trails_title)) },
        navigationIcon = {
            IconButton(onClick = onMainMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "")
            }
        },
        actions = {
            var expanded by remember { mutableStateOf(false) }
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
                        onClick = onImportClick,
                        text = {
                            Text(stringResource(id = R.string.recordings_menu_import))
                            Spacer(Modifier.weight(1f))
                        }
                    )
                }
            }
        }
    )
}