package com.peterlaurence.trekme.features.mapcreate.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.databinding.FragmentMapCreateBinding
import com.peterlaurence.trekme.features.common.presentation.ui.theme.m3.TrekMeTheme
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.MapSourceListViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * This fragment is used for displaying available WMTS map sources.
 *
 * @author P.Laurence on 08/04/18
 */
@AndroidEntryPoint
class MapCreateFragment : Fragment() {
    val viewModel: MapSourceListViewModel by navGraphViewModels(R.id.mapCreationGraph) {
        defaultViewModelProviderFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /* The action bar isn't managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = getString(R.string.mapcreate_title)
        }

        val binding = FragmentMapCreateBinding.inflate(inflater, container, false)
        binding.mapSourceListView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                TrekMeTheme {
                    MapSourceListStateful(viewModel) {
                        viewModel.setMapSource(it)
                        if (it == WmtsSource.IGN) {
                            navigateToOfferGateway()
                        } else {
                            navigateToWmtsFragment()
                        }
                    }
                }
            }
        }
        return binding.root
    }

    private fun navigateToWmtsFragment() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.mapCreateFragment) {
            val action =
                MapCreateFragmentDirections.actionMapCreateFragmentToWmtsFragment()
            navController.navigate(action)
        }
    }

    private fun navigateToOfferGateway() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.mapCreateFragment) {
            val action =
                MapCreateFragmentDirections.actionMapCreateFragmentToExtendedOfferGatewayFragment()
            navController.navigate(action)
        }
    }
}