package com.peterlaurence.trekme.features.about.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Displays the link on the user manual, and encourages the user to give feedback about the
 * application.
 */
@AndroidEntryPoint
class AboutFragment : Fragment() {
    @Inject
    lateinit var appEventBus: AppEventBus

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrekMeTheme {
                    AboutStateful(
                        onMainMenuClick = appEventBus::openDrawer
                    )
                }
            }
        }
    }
}