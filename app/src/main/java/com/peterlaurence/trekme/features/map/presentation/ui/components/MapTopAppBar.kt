package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.flowlayout.FlowMainAxisAlignment
import com.peterlaurence.trekme.features.common.presentation.ui.flowlayout.FlowRow
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTopAppBar(
    title: String,
    isShowingOrientation: Boolean,
    isShowingDistance: Boolean,
    isShowingDistanceOnTrack: Boolean,
    isShowingSpeed: Boolean,
    isLockedOnPosition: Boolean,
    isShowingGpsData: Boolean,
    hasBeacons: Boolean,
    hasTrackFollow: Boolean,
    onMenuClick: () -> Unit,
    onManageTracks: () -> Unit,
    onFollowTrack: () -> Unit,
    onToggleShowOrientation: () -> Unit,
    onAddMarker: () -> Unit,
    onAddLandmark: () -> Unit,
    onAddBeacon: () -> Unit,
    onShowDistance: () -> Unit,
    onToggleDistanceOnTrack: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleLockPosition: () -> Unit,
    onToggleShowGpsData: () -> Unit,
    onNavigateToShop: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    var expandedAddOnMap by remember { mutableStateOf(false) }
    var isShowingTrackFollowHelp by rememberSaveable { mutableStateOf(false) }
    var isShowingTrackFollowRedirect by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
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
                            { modifier -> Marker(modifier.padding(top = 8.dp)) },
                            R.string.mapview_add_marker,
                            onClick = {
                                expandedAddOnMap = false
                                onAddMarker()
                            }
                        )
                        IconAndText(
                            { modifier -> LandMark(modifier.padding(top = 8.dp)) },
                            R.string.mapview_add_landmark,
                            onClick = {
                                expandedAddOnMap = false
                                onAddLandmark()
                            }
                        )

                        if (hasBeacons) {
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
            }
            IconButton(
                onClick = { expandedMenu = true },
                modifier = Modifier.width(36.dp)
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
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    offset = DpOffset(0.dp, 0.dp)
                ) {
                    DropdownMenuItem(
                        onClick = {
                            expandedMenu = false
                            onManageTracks()
                        },
                        text = {
                            Text(stringResource(id = R.string.manage_tracks_menu))
                            Spacer(Modifier.weight(1f))
                        }
                    )
                    DropdownMenuItem(
                        onClick = {
                            expandedMenu = false
                            if (hasTrackFollow) {
                                onFollowTrack()
                            } else {
                                isShowingTrackFollowRedirect = true
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.follow_track_menu))
                                Spacer(Modifier.weight(1f))
                                if (hasTrackFollow) {
                                    IconButton(onClick = { isShowingTrackFollowHelp = true }) {
                                        Image(
                                            painter = painterResource(id = R.drawable.help_circle_outline),
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary)
                                        )
                                    }
                                } else {
                                    IconButton(onClick = { isShowingTrackFollowRedirect = true }) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_lock),
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary)
                                        )
                                    }
                                }
                            }
                        }
                    )
                    DropdownMenuItem(
                        onClick = onToggleSpeed,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.mapview_show_speed))
                                Spacer(Modifier.weight(1f))
                                Checkbox(
                                    checked = isShowingSpeed,
                                    onCheckedChange = { onToggleSpeed() }
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        onClick = onShowDistance,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.mapview_measure_distance))
                                Spacer(Modifier.weight(1f))
                                Checkbox(
                                    checked = isShowingDistance,
                                    onCheckedChange = { onShowDistance() }
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        onClick = onToggleDistanceOnTrack,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.mapview_measure_distance_on_track))
                                Spacer(Modifier.weight(1f))
                                Checkbox(
                                    checked = isShowingDistanceOnTrack,
                                    onCheckedChange = { onToggleDistanceOnTrack() }
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        onClick = onToggleLockPosition,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.mapview_lock_on_position))
                                Spacer(Modifier.weight(1f))
                                Checkbox(
                                    checked = isLockedOnPosition,
                                    onCheckedChange = { onToggleLockPosition() }
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        onClick = onToggleShowOrientation,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.mapview_orientation_enable))
                                Spacer(Modifier.weight(1f))
                                Checkbox(
                                    checked = isShowingOrientation,
                                    onCheckedChange = { onToggleShowOrientation() }
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        onClick = onToggleShowGpsData,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.mapview_gpsdata_enable))
                                Spacer(Modifier.weight(1f))
                                Checkbox(
                                    checked = isShowingGpsData,
                                    onCheckedChange = { onToggleShowGpsData() }
                                )
                            }
                        }
                    )
                }
            }
        }
    )

    if (isShowingTrackFollowHelp) {
        AlertDialog(
            onDismissRequest = { isShowingTrackFollowHelp = false },
            text = {
                Text(text = stringResource(id = R.string.track_follow_help))
            },
            confirmButton = {
                TextButton(onClick = { isShowingTrackFollowHelp = false }) {
                    Text(text = stringResource(id = R.string.ok_dialog))
                }
            },
        )
    }

    if (isShowingTrackFollowRedirect) {
        AlertDialog(
            onDismissRequest = { isShowingTrackFollowRedirect = false },
            title = {
                Text(stringResource(id = R.string.map_settings_trekme_extended_title))
            },
            text = {
                Text(text = stringResource(id = R.string.track_follow_help))
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isShowingTrackFollowRedirect = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.cancel_dialog_string))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isShowingTrackFollowRedirect = false
                        onNavigateToShop()
                    }
                ) {
                    Text(text = stringResource(id = R.string.ok_dialog))
                }
            },
        )
    }
}

@Composable
private fun IconAndText(icon: @Composable (Modifier) -> Unit, textId: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(90.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
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
            { modifier -> Marker(modifier.padding(top = 8.dp)) },
            R.string.mapview_add_marker,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun IconAndTextPreview2() {
    TrekMeTheme {
        IconAndText(
            { modifier -> Beacon(modifier, beaconVicinityRadiusPx = 50f) },
            R.string.mapview_add_marker,
            onClick = {}
        )
    }
}