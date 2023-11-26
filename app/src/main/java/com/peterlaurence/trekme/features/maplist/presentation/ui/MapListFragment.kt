package com.peterlaurence.trekme.features.maplist.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.maplist.presentation.ui.navigation.MapListGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Shows the list of maps.
 */
@AndroidEntryPoint
class MapListFragment : Fragment() {

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

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                TrekMeTheme {
                    MapListGraph(
                        onNavigateToMapCreate = {
                            val navController = findNavController()
                            navController.navigate(R.id.action_global_mapCreateGraph)
                                                },
                        onNavigateToMap = { mapId ->
                            val navController = findNavController()
                            if (navController.currentDestination?.id == R.id.mapListFragment) {
                                navController.navigate(R.id.action_global_mapFragment)
                            }
                        },
                        onNavigateToExcursionSearch = {
                            appEventBus.navigateTo(AppEventBus.NavDestination.TrailSearch)
                        }
                    )
                }
            }
        }
    }
}