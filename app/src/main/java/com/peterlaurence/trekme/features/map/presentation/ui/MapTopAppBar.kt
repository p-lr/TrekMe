package com.peterlaurence.trekme.features.map.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R

@Composable
fun MapTopAppBar(
    isShowingOrientation: Boolean,
    isShowingDistance: Boolean,
    isShowingSpeed: Boolean,
    isLockedOnPosition: Boolean,
    onMenuClick: () -> Unit,
    onToggleShowOrientation: () -> Unit,
    onAddMarker: () -> Unit,
    onAddLandmark: () -> Unit,
    onShowDistance: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleLockPosition: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "")
            }
        },
        actions = {
            IconButton(onClick = onAddMarker) {
                Icon(
                    painterResource(id = R.drawable.ic_add_location_white_24dp),
                    contentDescription = stringResource(id = R.string.mapview_add_marker),
                    tint = Color.White
                )
            }
            IconButton(onClick = onAddLandmark) {
                Icon(
                    painterResource(id = R.drawable.lighthouse_24dp),
                    contentDescription = stringResource(id = R.string.mapview_add_landmark),
                    tint = Color.White
                )
            }
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.width(36.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.White
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
                    DropdownMenuItem(onClick = onToggleSpeed) {
                        Text(stringResource(id = R.string.mapview_show_speed))
                        Spacer(Modifier.weight(1f))
                        Checkbox(
                            checked = isShowingSpeed,
                            onCheckedChange = { onToggleSpeed() })
                    }
                    DropdownMenuItem(onClick = onShowDistance) {
                        Text(stringResource(id = R.string.mapview_measure_distance))
                        Spacer(Modifier.weight(1f))
                        Checkbox(
                            checked = isShowingDistance,
                            onCheckedChange = { onShowDistance() })
                    }
                    DropdownMenuItem(onClick = onToggleLockPosition) {
                        Text(stringResource(id = R.string.mapview_lock_on_position))
                        Spacer(Modifier.weight(1f))
                        Checkbox(
                            checked = isLockedOnPosition,
                            onCheckedChange = { onToggleLockPosition() })
                    }
                    DropdownMenuItem(onClick = onToggleShowOrientation) {
                        Text(stringResource(id = R.string.mapview_orientation_enable))
                        Spacer(Modifier.weight(1f))
                        Checkbox(
                            checked = isShowingOrientation,
                            onCheckedChange = { onToggleShowOrientation() })
                    }
                }
            }
        }
    )

}