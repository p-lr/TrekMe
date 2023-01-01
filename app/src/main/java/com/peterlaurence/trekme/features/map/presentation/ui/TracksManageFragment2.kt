package com.peterlaurence.trekme.features.map.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.map.presentation.ui.screens.TracksManageStateful
import com.peterlaurence.trekme.features.map.presentation.viewmodel.TracksManageViewModel2
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TracksManageFragment2 : Fragment() {

    @Inject
    lateinit var mapFeatureEvents: MapFeatureEvents

    @Inject
    lateinit var appEventBus: AppEventBus

    val viewModel: TracksManageViewModel2 by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /* The action bar is managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            hide()
            title = ""
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrekMeTheme {
                    TracksManageStateful(
                        viewModel,
                        onGoToRoute = {
                            findNavController().navigateUp()
                            mapFeatureEvents.postGoToRoute(it)
                        },
                        onMenuClick = appEventBus::openDrawer
                    )
                }
            }
        }
    }
}