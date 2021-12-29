package com.peterlaurence.trekme.ui.mapview.components.statspanel

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.features.map.presentation.ui.components.StatsPanel
import com.peterlaurence.trekme.viewmodel.mapview.StatisticsViewModel


class StatsPanelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        val statisticsViewModel: StatisticsViewModel = viewModel()
        val stats by statisticsViewModel.stats.collectAsState(initial = null)

        val stats_ = stats ?: return

        StatsPanel(stats_)
    }
}
