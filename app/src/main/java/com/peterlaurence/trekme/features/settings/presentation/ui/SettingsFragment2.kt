package com.peterlaurence.trekme.features.settings.presentation.ui

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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment2 : Fragment() {
    @Inject
    lateinit var appEventBus: AppEventBus

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        /* The action bar is managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            hide()
            title = ""
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrekMeTheme {
                    SettingsStateful(
                        onMainMenuClick = appEventBus::openDrawer
                    )
                }
            }
        }
    }
}