package com.peterlaurence.trekme.features.map.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.databinding.FragmentBeaconEditBinding
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.map.domain.interactors.BeaconInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.screens.BeaconEditStateful
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BeaconEditFragment : Fragment() {
    private val args: BeaconEditFragmentArgs by navArgs()

    @Inject
    lateinit var beaconInteractor: BeaconInteractor

    @Inject
    lateinit var settings: Settings

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

        val binding = FragmentBeaconEditBinding.inflate(inflater, container, false)

        binding.beaconEditScreen.apply {
            setContent {
                TrekMeTheme {
                    BeaconEditStateful(
                        args.beacon,
                        args.mapId,
                        beaconInteractor,
                        settings,
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