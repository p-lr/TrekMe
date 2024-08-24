package com.peterlaurence.trekme.features.record.presentation.ui.components

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.flowlayout.FlowMainAxisAlignment
import com.peterlaurence.trekme.features.common.presentation.ui.flowlayout.FlowRow
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import kotlinx.parcelize.Parcelize
import java.util.*

@Composable
fun RecordItem(
    // Using lambda instead of direct reference to avoid unnecessary recompositions
    modifierProvider: () -> Modifier = { Modifier },
    item: SelectableRecordingItem,
    index: Int,
    onClick: () -> Unit = {}
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

    Column(
        modifierProvider()
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.isSelected) {
                Image(
                    painter = painterResource(id = R.drawable.check),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(14.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiaryContainer),
                    contentDescription = null
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(text = item.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (item.stats != null) {
            FlowRow(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                    .fillMaxWidth(),
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
                isSelected = false,
                id = UUID.randomUUID()
            ),
            index = 0
        )
    }
}