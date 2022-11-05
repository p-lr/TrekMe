package com.peterlaurence.trekme.features.map.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.flowlayout.FlowMainAxisAlignment
import com.peterlaurence.trekme.features.common.presentation.ui.flowlayout.FlowRow
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.map.presentation.ui.components.Beacon
import com.peterlaurence.trekme.features.map.presentation.ui.components.LandMark
import com.peterlaurence.trekme.features.map.presentation.ui.components.Marker

@Composable
fun MapTopAppBar(
    isShowingOrientation: Boolean,
    isShowingDistance: Boolean,
    isShowingDistanceOnTrack: Boolean,
    isShowingSpeed: Boolean,
    isLockedOnPosition: Boolean,
    isShowingGpsData: Boolean,
    onMenuClick: () -> Unit,
    onManageTracks: () -> Unit,
    onToggleShowOrientation: () -> Unit,
    onAddMarker: () -> Unit,
    onAddLandmark: () -> Unit,
    onAddBeacon: () -> Unit,
    onShowDistance: () -> Unit,
    onToggleDistanceOnTrack: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleLockPosition: () -> Unit,
    onToggleShowGpsData: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    var expandedAddOnMap by remember { mutableStateOf(false) }

    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "")
            }
        },
        actions = {
            IconButton(
                onClick = { expandedAddOnMap = true },
                modifier = Modifier.width(36.dp)
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_map_marker_plus),
                    contentDescription = stringResource(id = R.string.mapview_add_elements),
                    tint = Color.White
                )
            }

            Box(
                Modifier
                    .height(24.dp)
                    .wrapContentSize(Alignment.BottomEnd, true)
            ) {
                DropdownMenu(
                    expanded = expandedAddOnMap,
                    onDismissRequest = { expandedAddOnMap = false }
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        mainAxisAlignment = FlowMainAxisAlignment.SpaceEvenly,
                        tryAlign = true
                    ) {
                        IconAndText(
                            { modifier -> Marker(modifier) },
                            R.string.mapview_add_marker,
                            onClick = {
                                expandedAddOnMap = false
                                onAddMarker()
                            }
                        )
                        IconAndText(
                            { modifier -> LandMark(modifier) },
                            R.string.mapview_add_landmark,
                            onClick = {
                                expandedAddOnMap = false
                                onAddLandmark()
                            }
                        )
                        IconAndText(
                            { modifier ->
                                val radius = with(LocalDensity.current) { 20.dp.toPx() }
                                Beacon(modifier, beaconVicinityRadiusPx = radius)
                            },
                            R.string.mapview_add_beacon,
                            onClick = {
                                expandedAddOnMap = false
                                onAddBeacon()
                            }
                        )
                    }
                }
            }
            IconButton(
                onClick = { expandedMenu = true },
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
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    offset = DpOffset(0.dp, 0.dp)
                ) {
                    DropdownMenuItem(onClick = onManageTracks) {
                        Text(stringResource(id = R.string.manage_tracks_menu))
                        Spacer(Modifier.weight(1f))
                    }
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
                    DropdownMenuItem(onClick = onToggleDistanceOnTrack) {
                        Text(stringResource(id = R.string.mapview_measure_distance_on_track))
                        Spacer(Modifier.weight(1f))
                        Checkbox(
                            checked = isShowingDistanceOnTrack,
                            onCheckedChange = { onToggleDistanceOnTrack() })
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
                    DropdownMenuItem(onClick = onToggleShowGpsData) {
                        Text(stringResource(id = R.string.mapview_gpsdata_enable))
                        Spacer(Modifier.weight(1f))
                        Checkbox(
                            checked = isShowingGpsData,
                            onCheckedChange = { onToggleShowGpsData() })
                    }
                }
            }
        }
    )
}

@Composable
private fun IconAndText(icon: @Composable (Modifier) -> Unit, textId: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(90.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        icon(Modifier.align(Alignment.TopCenter))
        Text(stringResource(id = textId), Modifier.align(Alignment.BottomCenter))
    }
}

@Preview(showBackground = true)
@Composable
fun IconAndTextPreview() {
    TrekMeTheme {
        IconAndText(
            { modifier -> Marker(modifier) },
            R.string.mapview_add_marker,
            onClick = {}
        )
    }
}