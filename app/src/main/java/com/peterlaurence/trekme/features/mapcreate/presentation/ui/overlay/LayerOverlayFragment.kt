package com.peterlaurence.trekme.features.mapcreate.presentation.ui.overlay

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper.*
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.LayerOverlayViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

/**
 * User interface to add, remove, and reorder overlays.
 * Removal is done with a swipe gesture, while reordering is done using drag & drop using a handle
 * on the right-side.
 *
 * @since 2021-01-09
 */
@AndroidEntryPoint
class LayerOverlayFragment : Fragment() {
    val viewModel: LayerOverlayViewModel by viewModels()
    private var wmtsSource: WmtsSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wmtsSource = arguments?.let {
            LayerOverlayFragmentArgs.fromBundle(it)
        }?.wmtsSourceBundle?.wmtsSource
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        /* The action bar is managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            hide()
            title = ""
        }

        val wmtsSource = this.wmtsSource ?: return null

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrekMeTheme {
                    LayerOverlayStateful(
                        viewModel = viewModel,
                        wmtsSource = wmtsSource,
                        onBack = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}

@Parcelize
data class LayerOverlayDataBundle(val wmtsSource: WmtsSource): Parcelable
