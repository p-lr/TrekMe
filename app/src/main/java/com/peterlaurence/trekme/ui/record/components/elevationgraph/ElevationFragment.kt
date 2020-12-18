package com.peterlaurence.trekme.ui.record.components.elevationgraph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.LabelFormatter.LABEL_GONE
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.databinding.FragmentElevationBinding
import com.peterlaurence.trekme.repositories.recording.Calculating
import com.peterlaurence.trekme.repositories.recording.ElevationData
import com.peterlaurence.trekme.repositories.recording.NoNetwork
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
                        b.elevationGraphView.setPoints(config.points, config.eleMin, config.eleMax)
                        b.elevationTop.text = UnitFormatter.formatDistance(config.eleMax)
                        b.elevationBottom.text = UnitFormatter.formatDistance(config.eleMin)
                        b.elevationBottomTop.text = UnitFormatter.formatDistance(config.eleMax - config.eleMin)
                    }
                    NoNetwork -> TODO()
                    Calculating -> {
                        println("calculating")
                    }
                }
            }
        }

        view.post {
            viewModel.onUpdateGraph(b.elevationGraphView.getDrawingWidth())
        }

        b.slider.labelBehavior = LABEL_GONE
        b.slider.addOnChangeListener { _, value, _ ->
            b.elevationGraphView.setHighlightPt(value)
        }
    }
}