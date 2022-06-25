package com.peterlaurence.trekme.ui.record.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.flowlayout.FlowMainAxisAlignment
import com.peterlaurence.trekme.features.common.presentation.ui.flowlayout.FlowRow
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor

@Composable
fun RecordItem(
    name: String,
    stats: RecordStats? = null
) {
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Text(text = name, color = textColor())

        if (stats != null) {
            FlowRow(
                modifier = Modifier
                    .padding(start = 8.dp, top = 8.dp)
                    .fillMaxWidth(),
                mainAxisAlignment = FlowMainAxisAlignment.SpaceBetween,
                mainAxisSpacing = 20.dp,
                crossAxisSpacing = 8.dp,
                lastLineAligned = true
            ) {
                DistanceItem(stats.distance)
                ElevationUpStack(stats.elevationUpStack)
                ElevationDownStack(stats.elevationDownStack)
                ChronoItem(stats.duration)
                SpeedItem(stats.speed)
            }
        }
    }
}

data class RecordStats(
    val distance: String,
    val elevationUpStack: String,
    val elevationDownStack: String,
    val duration: String,
    val speed: String
)

@Composable
private fun DistanceItem(distance: String) {
    StatItem(
        icon = R.drawable.rule,
        text = distance,
        contentDescription = stringResource(id = R.string.distance_desc)
    )
}

@Composable
private fun ElevationUpStack(elevationUpStack: String) {
    StatItem(
        icon = R.drawable.elevation_up,
        text = elevationUpStack,
        contentDescription = stringResource(id = R.string.elevation_up_stack_desc)
    )
}

@Composable
private fun ElevationDownStack(elevationDownStack: String) {
    StatItem(
        icon = R.drawable.elevation_down,
        text = elevationDownStack,
        contentDescription = stringResource(id = R.string.elevation_down_stack_desc)
    )
}

@Composable
private fun ChronoItem(duration: String) {
    StatItem(
        icon = R.drawable.timer_18dp,
        text = duration,
        contentDescription = stringResource(id = R.string.duration_desc),
        tint = colorResource(id = R.color.colorAccent)
    )
}

@Composable
private fun SpeedItem(speed: String) {
    StatItem(
        icon = R.drawable.speedometer_medium_18dp,
        text = speed,
        contentDescription = stringResource(R.string.speed_desc),
        tint = colorResource(id = R.color.colorAccent)
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
        Text(text, modifier = Modifier.alpha(0.6f), color = textColor())
    }
}

@Preview(widthDp = 404, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun RecordItemPreview() {
    TrekMeTheme {
        RecordItem(
            name = "Track name",
            stats = RecordStats(
                "11.51 km",
                "+127 m",
                "-655 m",
                "2h46",
                "8.2 km/h"
            )
        )
    }
}