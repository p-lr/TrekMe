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
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MarkerEditFragment : Fragment() {
    private val args: MarkerEditFragmentArgs by navArgs()

    @Inject
    lateinit var mapFeatureEvents: MapFeatureEvents

    @Inject
    lateinit var mapInteractor: MapInteractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Uses an action bar made in Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMarkerEditBinding.inflate(inflater, container, false)

        binding.markerEditScreen.apply {
            setContent {
                TrekMeTheme {
                    MarkerEditScreen(
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