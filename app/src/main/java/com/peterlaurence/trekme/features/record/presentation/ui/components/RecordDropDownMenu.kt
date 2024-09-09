package com.peterlaurence.trekme.features.record.presentation.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R

@Composable
fun RecordDropDownMenu(
    expanded: Boolean,
    isTrackSharePending: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onChooseMap: () -> Unit,
    onShare: () -> Unit,
    onShowElevationGraph: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset((-16).dp, 0.dp)
    ) {
        DropdownMenuItem(
            onClick = {
                onDismiss()
                onSelect()
            },
            text = {
                Text(stringResource(id = R.string.track_select))
                Spacer(Modifier.weight(1f))
            },
            leadingIcon = {
                Icon(
                    painterResource(id = R.drawable.selection_ellipse),
                    modifier = Modifier.size(24.dp),
                    contentDescription = stringResource(
                        id = R.string.recording_edit_name_desc
                    )
                )
            }
        )
        DropdownMenuItem(
            onClick = {
                onDismiss()
                onRename()
            },
            text = {
                Text(stringResource(id = R.string.track_rename))
                Spacer(Modifier.weight(1f))
            },
            leadingIcon = {
                Icon(
                    painterResource(id = R.drawable.ic_edit_black_30dp),
                    modifier = Modifier.size(24.dp),
                    contentDescription = stringResource(
                        id = R.string.recording_edit_name_desc
                    )
                )
            }
        )
        DropdownMenuItem(
            onClick = {
                onDismiss()
                onChooseMap()
            },
            text = {
                Text(stringResource(id = R.string.track_choose_map))
                Spacer(Modifier.weight(1f))
            },
            leadingIcon = {
                Icon(
                    painterResource(id = R.drawable.import_24dp),
                    modifier = Modifier.size(24.dp),
                    contentDescription = stringResource(
                        id = R.string.recording_edit_name_desc
                    )
                )
            }
        )

        DropdownMenuItem(
            enabled = !isTrackSharePending,
            onClick = {
                onDismiss()
                onShare()
            },
            text = {
                Text(stringResource(id = R.string.track_share))
                Spacer(Modifier.weight(1f))
            },
            leadingIcon = {
                Icon(
                    painterResource(id = R.drawable.ic_share_black_24dp),
                    modifier = Modifier.size(24.dp),
                    contentDescription = stringResource(
                        id = R.string.recording_edit_name_desc
                    )
                )
            }
        )

        DropdownMenuItem(
            onClick = {
                onDismiss()
                onShowElevationGraph()
            },
            text = {
                Text(stringResource(id = R.string.track_elevation_profile))
                Spacer(Modifier.weight(1f))
            },
            leadingIcon = {
                Icon(
                    painterResource(id = R.drawable.elevation_graph),
                    modifier = Modifier.padding(bottom = 4.dp).size(24.dp),
                    contentDescription = stringResource(
                        id = R.string.recording_edit_name_desc
                    )
                )
            }
        )

        DropdownMenuItem(
            onClick = {
                onDismiss()
                onRemove()
            },
            text = {
                Text(stringResource(id = R.string.delete_dialog))
                Spacer(Modifier.weight(1f))
            },
            leadingIcon = {
                Icon(
                    painterResource(id = R.drawable.ic_delete_forever_black_24dp),
                    contentDescription = stringResource(
                        id = R.string.recording_edit_name_desc
                    )
                )
            }
        )
    }
}