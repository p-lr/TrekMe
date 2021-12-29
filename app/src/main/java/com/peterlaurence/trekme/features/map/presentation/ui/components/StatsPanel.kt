package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.track.TrackStatistics
import com.peterlaurence.trekme.core.ui.flowlayout.FlowMainAxisAlignment
import com.peterlaurence.trekme.core.ui.flowlayout.FlowRow
import com.peterlaurence.trekme.core.units.UnitFormatter

@Composable
fun StatsPanel(
    stats: TrackStatistics,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        mainAxisAlignment = FlowMainAxisAlignment.SpaceEvenly
    ) {
        StatWithImage(
            statText = UnitFormatter.formatDistance(stats.distance),
            imageId = R.drawable.ic_directions_walk_black_16dp
        )
        StatWithImage(
            statText = UnitFormatter.formatElevation(stats.elevationUpStack),
            imageId = R.drawable.elevation_up
        )
        StatWithImage(
            statText = UnitFormatter.formatElevation(stats.elevationDownStack),
            imageId = R.drawable.elevation_down
        )
        StatWithImage(
            statText = if (stats.durationInSecond != null) UnitFormatter.formatDuration(stats.durationInSecond) else "-",
            imageId = R.drawable.timer_16dp
        )
    }
}

@Composable
private fun StatWithImage(statText: String, @DrawableRes imageId: Int) {
    Row(
        Modifier
            .padding(start = 5.dp, end = 10.dp)
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = imageId),
            contentDescription = stringResource(id = R.string.distance)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = statText)
    }
}