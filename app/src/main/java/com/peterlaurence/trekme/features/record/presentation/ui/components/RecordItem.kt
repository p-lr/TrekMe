package com.peterlaurence.trekme.features.record.presentation.ui.components

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.peterlaurence.trekme.features.common.presentation.ui.theme.surfaceBackground
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor
import kotlinx.parcelize.Parcelize

@Composable
fun RecordItem(
    modifier: Modifier = Modifier,
    name: String,
    stats: RecordStats? = null,
    isSelected: Boolean,
    isMultiSelectionMode: Boolean,
    index: Int,
    onClick: () -> Unit = {}
) {
    val background = if (isSelected) {
        if (isSystemInDarkTheme()) Color(0xff3b5072) else Color(0xffc1d8ff)
    } else {
        if (index % 2 == 1) surfaceBackground() else {
            if (isSystemInDarkTheme()) Color(0xff3c3c3c) else Color(0x10000000)
        }
    }

    Column(
        modifier
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectionMode && isSelected) {
                Image(
                    painter = painterResource(id = R.drawable.check),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(14.dp),
                    colorFilter = ColorFilter.tint(colorResource(id = R.color.colorAccent)),
                    contentDescription = null
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(text = name, color = textColor())
        }

        if (stats != null) {
            FlowRow(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                    .fillMaxWidth(),
                mainAxisAlignment = FlowMainAxisAlignment.SpaceBetween,
                mainAxisSpacing = 20.dp,
                crossAxisSpacing = 8.dp,
                tryAlign = true
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
        Text(text, modifier = Modifier.alpha(0.7f), color = textColor())
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
            ),
            isSelected = false,
            isMultiSelectionMode = false,
            index = 0
        )
    }
}