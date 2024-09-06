@file:OptIn(ExperimentalFoundationApi::class)

package com.peterlaurence.trekme.features.record.presentation.ui.components

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.buttons.CustomIconButton
import com.peterlaurence.trekme.features.common.presentation.ui.flowlayout.FlowMainAxisAlignment
import com.peterlaurence.trekme.features.common.presentation.ui.flowlayout.FlowRow
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.record.presentation.ui.SelectableRecordingItem
import kotlinx.parcelize.Parcelize
import java.util.*

@Composable
fun RecordItem(
    // Using lambda instead of direct reference to avoid unnecessary recompositions
    modifierProvider: () -> Modifier = { Modifier },
    item: SelectableRecordingItem,
    index: Int,
    isMultiSelectionMode: Boolean,
    isTrackSharePending: Boolean,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onRename: () -> Unit = {},
    onChooseMap: () -> Unit = {},
    onShowElevationGraph: () -> Unit = {},
    onRemove: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    val background = if (item.isSelected) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        if (index % 2 == 1) {
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        } else {
            MaterialTheme.colorScheme.surface
        }
    }

    val paddingEnd = 62.dp

    Box(
        modifierProvider()
            .background(background)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            )
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Column {
            Text(
                text = item.name,
                modifier = Modifier.padding(start = 16.dp, end = paddingEnd),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.stats != null) {
                FlowRow(
                    modifier = Modifier
                        .padding(start = 16.dp, end = paddingEnd, top = 8.dp)
                        .fillMaxWidth()
                    ,
                    mainAxisAlignment = FlowMainAxisAlignment.SpaceBetween,
                    mainAxisSpacing = 20.dp,
                    crossAxisSpacing = 8.dp,
                    tryAlign = true
                ) {
                    DistanceItem(item.stats.distance)
                    ElevationUpStack(item.stats.elevationUpStack)
                    ElevationDownStack(item.stats.elevationDownStack)
                    ChronoItem(item.stats.duration)
                    SpeedItem(item.stats.speed)
                }
            }
        }

        if (isMultiSelectionMode) {
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .align(Alignment.CenterEnd),
            ) {
                Icon(
                    painter = if (item.isSelected) {
                        painterResource(id = R.drawable.check)
                    } else {
                        painterResource(id = R.drawable.check_circle_outline)
                    },
                    tint = MaterialTheme.colorScheme.tertiary,
                    contentDescription = null,
                )
            }
        } else {
            var expanded by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterEnd)
                    .wrapContentSize(Alignment.CenterEnd, true)
            ) {
                /* Since IconButton doesn't allow for a custom click size, we're using a custom
                 * button. */
                CustomIconButton(
                    onClick = { expanded = true },
                    size = 64.dp
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                    )
                }
                RecordDropDownMenu(
                    expanded = expanded,
                    isTrackSharePending = isTrackSharePending,
                    onRename = onRename,
                    onChooseMap = onChooseMap,
                    onShare = onShare,
                    onShowElevationGraph = onShowElevationGraph,
                    onRemove = onRemove,
                    onDismiss = { expanded = false }
                )
            }
        }
    }
}

@Parcelize
data class RecordStats(
    val distance: String,
    val elevationUpStack: String,
    val elevationDownStack: String,
    val duration: String,
    val speed: String
) : Parcelable

@Composable
private fun DistanceItem(distance: String) {
    StatItem(
        icon = R.drawable.rule,
        text = distance,
        tint = MaterialTheme.colorScheme.secondary,
        contentDescription = stringResource(id = R.string.distance_desc)
    )
}

@Composable
private fun ElevationUpStack(elevationUpStack: String) {
    StatItem(
        icon = R.drawable.elevation_up,
        text = elevationUpStack,
        tint = MaterialTheme.colorScheme.secondary,
        contentDescription = stringResource(id = R.string.elevation_up_stack_desc)
    )
}

@Composable
private fun ElevationDownStack(elevationDownStack: String) {
    StatItem(
        icon = R.drawable.elevation_down,
        text = elevationDownStack,
        tint = MaterialTheme.colorScheme.secondary,
        contentDescription = stringResource(id = R.string.elevation_down_stack_desc)
    )
}

@Composable
private fun ChronoItem(duration: String) {
    StatItem(
        icon = R.drawable.timer_18dp,
        text = duration,
        contentDescription = stringResource(id = R.string.duration_desc),
        tint = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun SpeedItem(speed: String) {
    StatItem(
        icon = R.drawable.speedometer_medium_18dp,
        text = speed,
        contentDescription = stringResource(R.string.speed_desc),
        tint = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun StatItem(
    @DrawableRes icon: Int,
    text: String,
    contentDescription: String,
    tint: Color? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            colorFilter = tint?.let {
                ColorFilter.tint(it)
            }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(widthDp = 404, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun RecordItemPreview() {
    TrekMeTheme {
        RecordItem(
            item = SelectableRecordingItem(
                "Track name",
                stats = RecordStats(
                    "11.51 km",
                    "+127 m",
                    "-655 m",
                    "2h46",
                    "8.2 km/h"
                ),
                isSelected = true,
                id = UUID.randomUUID().toString()
            ),
            isMultiSelectionMode = true,
            isTrackSharePending = false,
            index = 0
        )
    }
}

@Preview(widthDp = 404, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun RecordItemPreview2() {
    TrekMeTheme {
        RecordItem(
            item = SelectableRecordingItem(
                "Track name",
                stats = RecordStats(
                    "11.51 km",
                    "+127 m",
                    "-655 m",
                    "2h46",
                    "8.2 km/h"
                ),
                isSelected = false,
                id = UUID.randomUUID().toString()
            ),
            isMultiSelectionMode = false,
            isTrackSharePending = false,
            index = 0
        )
    }
}