package com.peterlaurence.trekme.features.maplist.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.maplist.presentation.ui.screens.CalibrationStateful
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.CalibrationViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CalibrationFragment2 : Fragment() {
    val viewModel: CalibrationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrekMeTheme {
                    CalibrationStateful(
                        viewModel,
                        onBackClick = { findNavController().navigateUp() })
                }
            }
        }
    }
}