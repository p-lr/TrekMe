package com.peterlaurence.trekme.ui.record.components.elevationgraph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.LabelFormatter.LABEL_GONE
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.databinding.FragmentElevationBinding
import com.peterlaurence.trekme.repositories.recording.Calculating
import com.peterlaurence.trekme.repositories.recording.ElePoint
import com.peterlaurence.trekme.repositories.recording.ElevationData
import com.peterlaurence.trekme.util.gpx.model.ElevationSource
import com.peterlaurence.trekme.util.px
import com.peterlaurence.trekme.viewmodel.record.ElevationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

/**
 * Shows an elevation graph along with:
 * * The lowest and highest elevations
 * * The difference between the highest and lowest elevations
 *
 * @author P.Laurence on 13/12/20
 */
@AndroidEntryPoint
class ElevationFragment : Fragment() {
    private var binding: FragmentElevationBinding? = null

    private val viewModel: ElevationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val b = FragmentElevationBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val b = binding ?: return

        /* Compute the usable size of the graph. We know that we use the entire screen width, so
         * the usable size is screen_width minus padding. */
        val graphUsableWidth = resources.configuration.screenWidthDp.px - b.elevationGraphView.getDrawingPadding()

        lifecycleScope.launchWhenResumed {
            viewModel.elevationPoints.collect { config ->
                when (config) {
                    is ElevationData -> {
                        if (config.points.isNotEmpty()) {
                            lifecycleScope.launchWhenStarted {
                                val points = withContext(Dispatchers.Default) {
                                    config.points.subSample(graphUsableWidth)
                                }
                                b.showGraph(true)
                                b.elevationGraphView.setPoints(points, config.eleMin, config.eleMax)
                                b.elevationTop.text = UnitFormatter.formatElevation(config.eleMax)
                                b.elevationBottom.text = UnitFormatter.formatElevation(config.eleMin)
                                b.elevationBottomTop.text = UnitFormatter.formatElevation(config.eleMax - config.eleMin)
                                b.elevationSrcTxt.text = when (config.elevationSource) {
                                    ElevationSource.GPS -> getString(R.string.elevation_src_gps)
                                    ElevationSource.IGN_RGE_ALTI -> getString(R.string.elevation_src_ign_rge_alti)
                                    ElevationSource.UNKNOWN -> getString(R.string.elevation_src_unknown)
                                }
                            }
                        } else {
                            b.showGraph(false)
                            b.loadingMsg.text = getString(R.string.no_elevations)
                        }
                    }
                    Calculating -> {
                        b.showGraph(false)
                        b.progressBar.visibility = View.VISIBLE
                        b.loadingMsg.text = getString(R.string.elevation_compute_in_progress)
                    }
                }
            }
        }

        viewModel.onUpdateGraph()

        b.slider.labelBehavior = LABEL_GONE
        b.slider.addOnChangeListener { _, value, _ ->
            b.elevationGraphView.setHighlightPt(value)
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
                val dist = chunk.sumByDouble { it.dist } / chunk.size
                val ele = chunk.sumByDouble { it.elevation } / chunk.size
                ElePoint(dist, ele)
            }
        } else this
    }
}