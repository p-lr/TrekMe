package com.peterlaurence.trekme.features.maplist.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.maplist.presentation.ui.screens.MapSettingsStateful
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.MapSettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapSettingsFragment2 : Fragment() {
    private val viewModel: MapSettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrekMeTheme {
                    MapSettingsStateful(
                        viewModel,
                        onNavigateToCalibration = { findNavController().navigate(MapSettingsFragment2Directions.actionMapSettingsFragment2ToCalibrationFragment2())},
                        onBackClick = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}