package com.peterlaurence.trekme.features.record.presentation.ui


import android.os.Bundle
import android.os.ParcelUuid
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordViewModel
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordingStatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class RecordFragment : Fragment() {

    val viewModel: RecordViewModel by activityViewModels()
    private val statViewModel: RecordingStatisticsViewModel by activityViewModels()

    @Inject
    lateinit var appEventBus: AppEventBus

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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
                    RecordScreen(
                        statViewModel = statViewModel,
                        recordViewModel = viewModel,
                        onElevationGraphClick = { data ->
                            val action =
                                RecordFragmentDirections.actionRecordFragmentToElevationFragment(
                                    ParcelUuid(data.id)
                                )
                            findNavController().navigate(action)
                        },
                        onGoToTrailSearchClick = { appEventBus.navigateTo(AppEventBus.NavDestination.TrailSearch) }
                    )
                }
            }
        }
    }
}
