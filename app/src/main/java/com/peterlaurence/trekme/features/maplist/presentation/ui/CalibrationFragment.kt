package com.peterlaurence.trekme.features.maplist.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.FragmentCalibrationBinding
import com.peterlaurence.trekme.features.maplist.presentation.ui.screens.CalibrationStateful
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.CalibrationViewModel
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CalibrationFragment : Fragment() {
    val viewModel: CalibrationViewModel by navGraphViewModels(R.id.map_settings_graph) {
        defaultViewModelProviderFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentCalibrationBinding.inflate(inflater, container, false)
        binding.calibrationView.setContent {
            TrekMeTheme {
                CalibrationStateful(viewModel)
            }
        }

        return binding.root
    }
}