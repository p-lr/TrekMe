package com.peterlaurence.trekme.features.map.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.peterlaurence.trekme.databinding.FragmentMarkerEditBinding
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.map.presentation.ui.screens.MarkerEditStateful
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MarkerEditFragment : Fragment() {
    private val args: MarkerEditFragmentArgs by navArgs()

    @Inject
    lateinit var mapFeatureEvents: MapFeatureEvents

    @Inject
    lateinit var mapInteractor: MapInteractor

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

        val binding = FragmentMarkerEditBinding.inflate(inflater, container, false)

        binding.markerEditScreen.apply {
            setContent {
                TrekMeTheme {
                    MarkerEditStateful(
                        args.marker,
                        args.mapId,
                        args.markerId,
                        mapFeatureEvents,
                        mapInteractor,
                        onBackAction = {
                            findNavController().navigateUp()
                        }
                    )
                }
            }
        }

        return binding.root
    }
}