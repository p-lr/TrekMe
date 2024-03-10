package com.peterlaurence.trekme.features.record.presentation.ui.components.elevationgraph

import android.view.View
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.slider.LabelFormatter
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.databinding.FragmentElevationBinding
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource
import com.peterlaurence.trekme.features.record.domain.model.Calculating
import com.peterlaurence.trekme.features.record.domain.model.ElePoint
import com.peterlaurence.trekme.features.record.domain.model.ElevationData
import com.peterlaurence.trekme.features.record.domain.model.ElevationState
import com.peterlaurence.trekme.features.record.domain.model.NoElevationData
import com.peterlaurence.trekme.features.record.presentation.viewmodel.ElevationViewModel
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        ElevationGraph(Modifier.padding(paddingValues), elevationState)
    }
}

/**
 * Shows an elevation graph along with:
 * * The lowest and highest elevations
 * * The difference between the highest and lowest elevations
 *
 * @since 2020/12/13
 */
@Composable
private fun ElevationGraph(modifier: Modifier, elevationState: ElevationState) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = context.resources

    AndroidViewBinding(
        factory = { inflater, vg, attach ->
            FragmentElevationBinding.inflate(inflater, vg, attach).also { b ->
                b.slider.labelBehavior = LabelFormatter.LABEL_GONE
                b.slider.addOnChangeListener { _, value, _ ->
                    b.elevationGraphView.setHighlightPt(value)
                }
            }
        },
        modifier = modifier
    ) {
        when (elevationState) {
            is ElevationData -> {
                if (elevationState.segmentElePoints.isNotEmpty()) {
                    /* Compute the usable size of the graph. We know that we use the entire screen width, so
                     * the usable size is screen_width minus padding. */
                    val graphUsableWidth =
                        dpToPx(resources.configuration.screenWidthDp.toFloat()) - elevationGraphView.getDrawingPadding()

                    scope.launch {
                        val points = withContext(Dispatchers.Default) {
                            /* For instance, represent all segments on the same line, one after another */
                            elevationState.segmentElePoints.flatMap { it.points }
                                .subSample(graphUsableWidth.toInt())
                        }
                        showGraph(true)
                        elevationGraphView.setPoints(
                            points,
                            elevationState.eleMin,
                            elevationState.eleMax
                        )
                        elevationTop.text = UnitFormatter.formatElevation(elevationState.eleMax)
                        elevationBottom.text =
                            UnitFormatter.formatElevation(elevationState.eleMin)
                        elevationBottomTop.text =
                            UnitFormatter.formatElevation(elevationState.eleMax - elevationState.eleMin)
                        elevationSrcTxt.text = when (elevationState.elevationSource) {
                            ElevationSource.GPS -> context.getString(R.string.elevation_src_gps)
                            ElevationSource.IGN_RGE_ALTI -> context.getString(R.string.elevation_src_ign_rge_alti)
                            ElevationSource.UNKNOWN -> context.getString(R.string.elevation_src_unknown)
                        }
                    }
                } else {
                    showGraph(false)
                    loadingMsg.text = context.getString(R.string.no_elevations)
                }
            }

            Calculating -> {
                showGraph(false)
                progressBar.visibility = View.VISIBLE
                loadingMsg.text = context.getString(R.string.elevation_compute_in_progress)
            }

            NoElevationData -> {
                showGraph(false)
                loadingMsg.text = context.getString(R.string.no_ele_profile_data)
            }
        }
    }
}

private fun FragmentElevationBinding.showGraph(b: Boolean) {
    graphLayout.visibility = if (b) View.VISIBLE else View.GONE
    loadingPanel.visibility = if (!b) View.VISIBLE else View.GONE
    progressBar.visibility = View.GONE
    loadingMsg.visibility = if (!b) View.VISIBLE else View.GONE
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