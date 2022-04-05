package com.peterlaurence.trekme.features.map.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.fragment.findNavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.FragmentMapBinding
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MapFragment : Fragment() {
    @Inject
    lateinit var mapFeatureEvents: MapFeatureEvents


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /* The action bar is managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        /* Handle navigation events */
        viewLifecycleOwner.lifecycleScope.launch {
            mapFeatureEvents.navigateToMarkerEdit.collect { (marker, mapId, markerId) ->
                val action = MapFragmentDirections.actionMapFragmentToMarkerEditFragment(marker, mapId, markerId)
                findNavController().navigate(action)
            }
        }

        val binding = FragmentMapBinding.inflate(inflater, container, false)

        binding.mapScreen.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            val onNavigateToTracksManage = {
                val action = MapFragmentDirections.actionMapFragmentToTracksManageFragment()
                findNavController().navigate(action)
            }

            setContent {
                /* Locally override the background color (dark theme or not). Done this way, it
                 * doesn't add a GPU overdraw. */
                TrekMeTheme(background = Color.White) {
                    /* By changing the view-model store owner to the activity in the current
                     * composition tree (which in this case starts at setContent { .. }) in the
                     * fragment, calling viewModel() inside a composable will provide us a
                     * view-model scoped to the activity.
                     * When this fragment layer will be removed, don't keep that CompositionLocalProvider,
                     * since the composition tree will start at the activity - so this won't be needed
                     * anymore. */
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner provides requireActivity()
                    ) {
                        MapScreen(onNavigateToTracksManage = onNavigateToTracksManage)
                    }
                }
            }
        }
        return binding.root
    }
}