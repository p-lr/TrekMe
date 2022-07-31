package com.peterlaurence.trekme.features.record.presentation.ui.components.elevationgraph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.google.android.material.slider.LabelFormatter.LABEL_GONE
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.databinding.FragmentElevationBinding
import com.peterlaurence.trekme.features.record.domain.repositories.Calculating
import com.peterlaurence.trekme.features.record.domain.repositories.ElePoint
import com.peterlaurence.trekme.features.record.domain.repositories.ElevationData
import com.peterlaurence.trekme.features.record.domain.repositories.NoElevationData
import com.peterlaurence.trekme.util.dpToPx
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource
import com.peterlaurence.trekme.features.record.presentation.viewmodel.ElevationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val args: ElevationFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        /* The action bar isn't managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = getString(R.string.elevation_profile_title)
        }

        val b = FragmentElevationBinding.inflate(inflater, container, false)
        binding = b
        collectElevationPoints()
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val b = binding ?: return

        viewModel.onUpdateGraph(args.id.uuid)

        b.slider.labelBehavior = LABEL_GONE
        b.slider.addOnChangeListener { _, value, _ ->
            b.elevationGraphView.setHighlightPt(value)
        }
    }

    private fun collectElevationPoints() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.elevationPoints.collect { config ->
                    val b = binding ?: return@collect
                    when (config) {
                        is ElevationData -> {
                            if (config.segmentElePoints.isNotEmpty()) {
                                /* Compute the usable size of the graph. We know that we use the entire screen width, so
                                 * the usable size is screen_width minus padding. */
                                val graphUsableWidth = dpToPx(resources.configuration.screenWidthDp.toFloat()) - b.elevationGraphView.getDrawingPadding()

                                val points = withContext(Dispatchers.Default) {
                                    /* For instance, represent all segments on the same line, one after another */
                                    config.segmentElePoints.flatMap { it.points }
                                        .subSample(graphUsableWidth.toInt())
                                }
                                b.showGraph(true)
                                b.elevationGraphView.setPoints(points, config.eleMin, config.eleMax)
                                b.elevationTop.text = UnitFormatter.formatElevation(config.eleMax)
                                b.elevationBottom.text =
                                    UnitFormatter.formatElevation(config.eleMin)
                                b.elevationBottomTop.text =
                                    UnitFormatter.formatElevation(config.eleMax - config.eleMin)
                                b.elevationSrcTxt.text = when (config.elevationSource) {
                                    ElevationSource.GPS -> getString(R.string.elevation_src_gps)
                                    ElevationSource.IGN_RGE_ALTI -> getString(R.string.elevation_src_ign_rge_alti)
                                    ElevationSource.UNKNOWN -> getString(R.string.elevation_src_unknown)
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
                        NoElevationData -> {
                            b.showGraph(false)
                            b.loadingMsg.text = getString(R.string.no_ele_profile_data)
                        }
                    }
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}