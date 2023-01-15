package com.peterlaurence.trekme.features.map.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.peterlaurence.trekme.databinding.FragmentMapBinding
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.map.presentation.ui.navigation.MapGraph
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MapFragment : Fragment() {
    @Inject
    lateinit var mapFeatureEvents: MapFeatureEvents

    @Inject
    lateinit var appEventBus: AppEventBus

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

        /* Handle navigation events */
        viewLifecycleOwner.lifecycleScope.launch {
            mapFeatureEvents.navigateToMarkerEdit.collect { (marker, mapId) ->
                val action = MapFragmentDirections.actionMapFragmentToMarkerEditFragment(marker, mapId)
                findNavController().navigate(action)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            mapFeatureEvents.navigateToBeaconEdit.collect { (beacon, mapId) ->
                val action = MapFragmentDirections.actionMapFragmentToBeaconEditFragment(beacon, mapId)
                findNavController().navigate(action)
            }
        }

        val binding = FragmentMapBinding.inflate(inflater, container, false)

        binding.mapScreen.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                MapGraph(
                    onMenuClick = appEventBus::openDrawer
                )
            }
        }
        return binding.root
    }
}