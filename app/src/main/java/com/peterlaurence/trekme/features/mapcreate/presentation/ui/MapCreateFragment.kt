package com.peterlaurence.trekme.features.mapcreate.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.navigation.MapCreateGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * This fragment is used for displaying available WMTS map sources.
 *
 * @since 08/04/18
 */
@AndroidEntryPoint
class MapCreateFragment : Fragment() {

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
                    MapCreateGraph(
                        onMenuClick = { appEventBus.openDrawer() },
                        onNavigateToShop = { appEventBus.navigateTo(AppEventBus.NavDestination.Shop) }
                    )
                }
            }
        }
    }
}