package com.peterlaurence.trekme.features.trailsearch.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.trailsearch.presentation.ui.navigation.TrailSearchGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrailSearchFragment : Fragment() {
    @Inject
    lateinit var appEventBus: AppEventBus

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                TrekMeTheme {
                    TrailSearchGraph(
                        onGoToMapList = {
                            appEventBus.navigateTo(AppEventBus.NavDestination.MapList)
                        },
                        onGoToShop = {
                            appEventBus.navigateTo(AppEventBus.NavDestination.Shop)
                        },
                        onGoToMapCreation = {
                           appEventBus.navigateTo(AppEventBus.NavDestination.MapCreation)
                        }
                    )
                }
            }
        }
    }
}