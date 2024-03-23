package com.peterlaurence.trekme.features.record.presentation.ui.components.elevationgraph

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource
import com.peterlaurence.trekme.features.record.domain.model.Calculating
import com.peterlaurence.trekme.features.record.domain.model.ElePoint
import com.peterlaurence.trekme.features.record.domain.model.ElevationData
import com.peterlaurence.trekme.features.record.domain.model.ElevationState
import com.peterlaurence.trekme.features.record.domain.model.NoElevationData
import com.peterlaurence.trekme.features.record.presentation.viewmodel.ElevationViewModel
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shows an elevation graph along with:
 * * The lowest and highest elevations
 * * The difference between the highest and lowest elevations
 */
@Composable
fun ElevationStateful(
    viewModel: ElevationViewModel,
    onBack: () -> Unit
) {
    val elevationState by viewModel.elevationState.collectAsStateWithLifecycle()

    ElevationScreen(elevationState, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElevationScreen(
    elevationState: ElevationState,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.elevation_profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                    }
                },
            )
        },
    ) { paddingValues ->
        val modifier = Modifier.padding(paddingValues)
        when (elevationState) {
            Calculating -> ProgressScreen(modifier)
            is ElevationData -> ElevationGraph(modifier, elevationState)
            NoElevationData -> ErrorScreen(modifier)
        }
    }
}

@Composable
private fun ElevationGraph(modifier: Modifier, elevationData: ElevationData) {
    Column(
        modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElevationStatistic(
                resId = R.drawable.elevation_top,
                descriptionId = R.string.elevation_top_icon,
                stat = elevationData.eleMax
            )
            ElevationStatistic(
                resId = R.drawable.elevation_bottom,
                descriptionId = R.string.elevation_bottom_icon,
                stat = elevationData.eleMin
            )
            ElevationStatistic(
                resId = R.drawable.elevation_bottom_top,
                descriptionId = R.string.elevation_bottom_top_icon,
                stat = elevationData.eleMax - elevationData.eleMin
            )
        }

        val context = LocalContext.current
        val resources = context.resources
        var sliderPosition by remember { mutableFloatStateOf(0.5f) }

        val points: List<ElePoint> by produceState(
            emptyList(),
            key1 = elevationData.segmentElePoints,
        ) {
            /* Compute the usable width in pixels of the graph. We know that we use the entire
             * screen width, so the usable width is screen_width minus padding. */
            val graphUsableWidth =
                dpToPx(resources.configuration.screenWidthDp.toFloat()) - ElevationGraphView.getDrawingPadding()

            value = withContext(Dispatchers.Default) {
                /* For instance, represent all segments on the same line, one after another */
                elevationData.segmentElePoints.flatMap { it.points }
                    .subSample(graphUsableWidth.toInt())
            }
        }

        key(points) {
            AndroidView(
                factory = { ctx ->
                    ElevationGraphView(ctx).also {
                        it.setPoints(
                            points,
                            elevationData.eleMin,
                            elevationData.eleMax
                        )
                    }
                },
                modifier = Modifier.height((resources.configuration.screenHeightDp / 3).dp)
            ) { view ->
                view.setHighlightPt(sliderPosition)
            }
        }

        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Text(
            text = stringResource(id = R.string.elevation_src_label),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = when (elevationData.elevationSource) {
                ElevationSource.GPS -> stringResource(R.string.elevation_src_gps)
                ElevationSource.IGN_RGE_ALTI -> stringResource(R.string.elevation_src_ign_rge_alti)
                ElevationSource.UNKNOWN -> stringResource(R.string.elevation_src_unknown)
            },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ElevationStatistic(resId: Int, descriptionId: Int, stat: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = resId),
            modifier = Modifier.size(48.dp),
            contentDescription = stringResource(id = descriptionId)
        )
        Text(text = UnitFormatter.formatElevation(stat))
    }
}

@Composable
private fun ProgressScreen(modifier: Modifier) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .width(100.dp)
                .align(Alignment.CenterHorizontally),
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.elevation_compute_in_progress),
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun ErrorScreen(modifier: Modifier) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.no_ele_profile_data),
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

private fun List<ElePoint>.subSample(targetWidth: Int): List<ElePoint> {
    val chunkSize = size / targetWidth
    return if (chunkSize >= 2) {
        chunked(chunkSize).map { chunk ->
            val dist = chunk.sumOf { it.dist } / chunk.size
            val ele = chunk.sumOf { it.elevation } / chunk.size
            ElePoint(dist, ele)
        }
    } else this
}