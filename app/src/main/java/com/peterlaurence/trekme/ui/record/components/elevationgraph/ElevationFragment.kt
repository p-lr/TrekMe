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
import com.peterlaurence.trekme.repositories.recording.ElevationCorrectionError
import com.peterlaurence.trekme.repositories.recording.ElevationData
import com.peterlaurence.trekme.repositories.recording.NoNetwork
import com.peterlaurence.trekme.util.px
import com.peterlaurence.trekme.viewmodel.record.ElevationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

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
        lifecycleScope.launchWhenResumed {
            viewModel.elevationPoints.collect { config ->
                when (config) {
                    is ElevationData -> {
                        b.showGraph(true)
                        b.elevationGraphView.setPoints(config.points, config.eleMin, config.eleMax)
                        b.elevationTop.text = UnitFormatter.formatElevation(config.eleMax)
                        b.elevationBottom.text = UnitFormatter.formatElevation(config.eleMin)
                        b.elevationBottomTop.text = UnitFormatter.formatElevation(config.eleMax - config.eleMin)
                    }
                    is NoNetwork -> {
                        b.showGraph(false)
                        if (config.restApiOk) {
                            b.loadingMsg.text = getString(R.string.network_required)
                        } else {
                            b.loadingMsg.text = getString(R.string.elevation_service_down)
                        }

                    }
                    ElevationCorrectionError -> {
                        b.showGraph(false)
                        b.loadingMsg.text = getString(R.string.elevation_correction_error)
                    }
                    Calculating -> {
                        b.showGraph(false)
                        b.progressBar.visibility = View.VISIBLE
                        b.loadingMsg.text = getString(R.string.elevation_compute_in_progress)
                    }
                }
            }
        }

        /* Compute the usable size of the graph. We know that we use the entire screen width, so
         * the usable size is screen_width minus padding. */
        val graphUsableWidth = resources.configuration.screenWidthDp.px - b.elevationGraphView.getDrawingPadding()
        viewModel.onUpdateGraph(graphUsableWidth)

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
}