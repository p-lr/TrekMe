package com.peterlaurence.trekme.features.map.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.peterlaurence.trekme.databinding.FragmentMapBinding
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMapBinding.inflate(inflater, container, false)

        binding.mapScreen.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                TrekMeTheme {
                    /* By changing the view-model store owner to the activity in the current
                     * composition tree (which in this case starts at setContent { .. }) in the
                     * fragment, calling viewModel() inside a composable will provide us a
                     * view-model scoped to the activity.
                     * When this fragment layer will be removed, don't keep that CompositionLocalProvider,
                     * since the composition tree will start at the activity - so this won't be needed
                     * anymore. */
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner provides requireActivity()
                    ) {
                        MapScreen()
                    }
                }
            }
        }
        return binding.root
    }
}