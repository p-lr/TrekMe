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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTopAppbar(
    selectionCount: Int,
    isTrackSharePending: Boolean,
    onMainMenuClick: () -> Unit,
    onImportClick: () -> Unit,
    onRename: () -> Unit,
    onChooseMap: () -> Unit,
    onShare: () -> Unit,
    onShowElevationGraph: () -> Unit,
    onRemove: () -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.my_trails_title)) },
        navigationIcon = {
            IconButton(onClick = onMainMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "")
            }
        },
        actions = {
            if (selectionCount == 1) {
                IconButton(
                    onClick = onRename,
                    modifier = Modifier.width(36.dp),
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_edit_black_24dp),
                        contentDescription = null,
                    )
                }
            }

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
                        onClick = {
                            onImportClick()
                            expanded = false
                        },
                        text = {
                            Text(stringResource(id = R.string.recordings_menu_import))
                            Spacer(Modifier.weight(1f))
                        },
                    )

                    if (selectionCount == 1) {
                        DropdownMenuItem(
                            onClick = onChooseMap,
                            text = {
                                Text(stringResource(id = R.string.track_choose_map))
                                Spacer(Modifier.weight(1f))
                            }
                        )
                    }

                    if (selectionCount > 0) {
                        DropdownMenuItem(
                            enabled = !isTrackSharePending,
                            onClick = {
                                onShare()
                                expanded = false
                            },
                            text = {
                                Text(stringResource(id = R.string.track_share))
                                Spacer(Modifier.weight(1f))
                            }
                        )
                    }

                    if (selectionCount == 1) {
                        DropdownMenuItem(
                            onClick = onShowElevationGraph,
                            text = {
                                Text(stringResource(id = R.string.track_elevation_profile))
                                Spacer(Modifier.weight(1f))
                            }
                        )
                    }

                    if (selectionCount > 0) {
                        DropdownMenuItem(
                            onClick = {
                                onRemove()
                                expanded = false
                            },
                            text = {
                                Text(stringResource(id = R.string.delete_dialog))
                                Spacer(Modifier.weight(1f))
                            }
                        )
                    }
                }
            }
        }
    )
}