package com.peterlaurence.trekme.ui.mapview.components.statspanel

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter.formatDistance
import com.peterlaurence.trekme.core.units.UnitFormatter.formatDuration
import com.peterlaurence.trekme.core.units.UnitFormatter.formatElevation
import com.peterlaurence.trekme.viewmodel.mapview.StatisticsViewModel

@Composable
fun StatWithImage(statText: String, @DrawableRes imageId: Int) {
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

class StatsPanel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        val statisticsViewModel: StatisticsViewModel = viewModel()
        val stats by statisticsViewModel.stats.collectAsState(initial = null)

        val stats_ = stats ?: return

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            mainAxisAlignment = FlowMainAxisAlignment.SpaceEvenly
        ) {
            StatWithImage(
                statText = formatDistance(stats_.distance),
                imageId = R.drawable.ic_directions_walk_black_16dp
            )
            StatWithImage(
                statText = formatElevation(stats_.elevationUpStack),
                imageId = R.drawable.elevation_up
            )
            StatWithImage(
                statText = formatElevation(stats_.elevationDownStack),
                imageId = R.drawable.elevation_down
            )
            StatWithImage(
                statText = if (stats_.durationInSecond != null) formatDuration(stats_.durationInSecond) else "-",
                imageId = R.drawable.timer_16dp
            )
        }
    }
}
