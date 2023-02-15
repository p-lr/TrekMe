package com.peterlaurence.trekme.features.record.presentation.ui


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.backgroundVariant
import com.peterlaurence.trekme.features.record.app.service.GpxRecordService
import com.peterlaurence.trekme.features.record.presentation.events.RecordEventBus
import com.peterlaurence.trekme.features.record.presentation.ui.components.ActionsStateful
import com.peterlaurence.trekme.features.record.presentation.ui.components.GpxRecordListStateful
import com.peterlaurence.trekme.features.record.presentation.ui.components.StatusStateful
import com.peterlaurence.trekme.features.record.presentation.viewmodel.GpxRecordServiceViewModel
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordViewModel
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordingStatisticsViewModel
import com.peterlaurence.trekme.util.collectWhileResumed
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Holds controls and displays information about the [GpxRecordService].
 * Displays the list of records (gpx files) along their statistics.
 *
 * @author P.Laurence -- converted to Kotlin on 01/11/18
 */
@AndroidEntryPoint
class RecordFragment : Fragment() {

    val viewModel: RecordViewModel by activityViewModels()
    private val statViewModel: RecordingStatisticsViewModel by activityViewModels()
    private val gpxRecordServiceViewModel: GpxRecordServiceViewModel by activityViewModels()

    @Inject
    lateinit var eventBus: RecordEventBus

    @Inject
    lateinit var appEventBus: AppEventBus

    private val importRecordingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                val uriList = if (uri != null) {
                    /* One file selected */
                    listOf(uri)
                } else {
                    val clipData = result.data?.clipData ?: return@registerForActivityResult
                    (0 until clipData.itemCount).map {
                        clipData.getItemAt(it).uri
                    }
                }
                statViewModel.importRecordings(uriList)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* This isn't a joke, we need to have the view-model instantiated now */
        viewModel

        eventBus.recordingDeletionFailedSignal.collectWhileResumed(this) {
            /* Alert the user that some files could not be deleted */
            val snackbar = Snackbar.make(
                requireView(), R.string.files_could_not_be_deleted,
                Snackbar.LENGTH_SHORT
            )
            snackbar.show()
        }

        appEventBus.geoRecordImportEvent.collectWhileResumed(this) {
            onGpxImported(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /* The action bar isn't managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = getString(R.string.recording_frgmt_title)
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrekMeTheme {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(backgroundVariant())
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                        ) {
                            ActionsStateful(
                                Modifier
                                    .weight(1f)
                                    .padding(top = 8.dp, start = 8.dp, end = 4.dp, bottom = 4.dp),
                                viewModel = gpxRecordServiceViewModel,
                                onStartStopClick = gpxRecordServiceViewModel::onStartStopClicked,
                                onPauseResumeClick = gpxRecordServiceViewModel::onPauseResumeClicked
                            )
                            StatusStateful(
                                Modifier
                                    .weight(1f)
                                    .padding(top = 8.dp, start = 4.dp, end = 8.dp, bottom = 4.dp),
                                viewModel = gpxRecordServiceViewModel
                            )
                        }

                        GpxRecordListStateful(
                            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                            statViewModel = statViewModel,
                            recordViewModel = viewModel,
                            onImportMenuClick = {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                                intent.addCategory(Intent.CATEGORY_OPENABLE)
                                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

                                /* Search for all documents available via installed storage providers */
                                intent.type = "*/*"
                                importRecordingsLauncher.launch(intent)
                            },
                            onShareRecords = { dataList ->
                                val activity = activity ?: return@GpxRecordListStateful
                                val intentBuilder = ShareCompat.IntentBuilder(activity)
                                    .setType("text/plain")
                                dataList.forEach {
                                    try {
                                        val uri = statViewModel.getRecordingUri(it)
                                        if (uri != null) {
                                            intentBuilder.addStream(uri)
                                        }
                                    } catch (e: IllegalArgumentException) {
                                        e.printStackTrace()
                                    }
                                }
                                intentBuilder.startChooser()
                            },
                            onElevationGraphClick = { data ->
                                val action =
                                    RecordFragmentDirections.actionRecordFragmentToElevationFragment(
                                        ParcelUuid(data.id)
                                    )
                                findNavController().navigate(action)
                            },
                            onDeleteClick = { dataList ->
                                statViewModel.onRequestDeleteRecordings(dataList)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun onGpxImported(event: GeoRecordImportResult) {
        val view = this.view ?: return
        when (event) {
            is GeoRecordImportResult.GeoRecordImportOk ->
                /* Tell the user that the track will be shortly available in the map */
                Snackbar.make(view, R.string.track_is_being_added, Snackbar.LENGTH_LONG)
                    .show()

            is GeoRecordImportResult.GeoRecordImportError ->
                /* Tell the user that an error occurred */
                Snackbar.make(view, R.string.track_add_error, Snackbar.LENGTH_LONG).show()
        }
    }

    companion object {
        const val TAG = "RecordFragment"
    }
}
