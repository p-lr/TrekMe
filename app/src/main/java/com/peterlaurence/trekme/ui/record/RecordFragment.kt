package com.peterlaurence.trekme.ui.record


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.databinding.FragmentRecordBinding
import com.peterlaurence.trekme.service.GpxRecordService
import com.peterlaurence.trekme.service.GpxRecordState
import com.peterlaurence.trekme.ui.record.components.RecordListView
import com.peterlaurence.trekme.ui.record.components.dialogs.BatteryOptWarningDialog
import com.peterlaurence.trekme.ui.record.components.dialogs.LocalisationDisclaimer
import com.peterlaurence.trekme.ui.record.components.dialogs.MapSelectionForImport
import com.peterlaurence.trekme.ui.record.components.dialogs.TrackFileNameEdit
import com.peterlaurence.trekme.ui.record.events.RecordEventBus
import com.peterlaurence.trekme.util.collectWhileResumed
import com.peterlaurence.trekme.util.collectWhileResumedIn
import com.peterlaurence.trekme.viewmodel.GpxRecordServiceViewModel
import com.peterlaurence.trekme.viewmodel.record.RecordViewModel
import com.peterlaurence.trekme.viewmodel.record.RecordingData
import com.peterlaurence.trekme.viewmodel.record.RecordingStatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Holds controls and displays information about the [GpxRecordService].
 * Displays the list of records (gpx files) along their statistics.
 *
 * @author P.Laurence -- converted to Kotlin on 01/11/18
 */
@AndroidEntryPoint
class RecordFragment : Fragment() {
    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    val viewModel: RecordViewModel by activityViewModels()
    val statViewModel: RecordingStatisticsViewModel by activityViewModels()
    private var recordingData: LiveData<List<RecordingData>>? = null

    @Inject
    lateinit var eventBus: RecordEventBus

    @Inject
    lateinit var appEventBus: AppEventBus

    private val importRecordingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
            binding.recordListView.onRecordingDeletionFail()
        }

        eventBus.showLocationDisclaimerSignal.collectWhileResumed(this) {
            LocalisationDisclaimer().show(parentFragmentManager, null)
        }

        appEventBus.gpxImportEvent.collectWhileResumed(this) {
            onGpxImported(it)
        }

        eventBus.disableBatteryOptSignal.collectWhileResumed(this) {
            BatteryOptWarningDialog().show(parentFragmentManager, null)
        }

        recordingData = statViewModel.getRecordingData()
        recordingData?.observe(this) {
            it?.let { data ->
                updateRecordingData(data)
            }
        }

        /**
         * Observe the changes in the [GpxRecordService] status, and update child views accordingly.
         */
        val gpxRecordServiceViewModel: GpxRecordServiceViewModel by activityViewModels()
        gpxRecordServiceViewModel.status.map { gpxRecordState ->
            dispatchGpxRecordServiceStatus(gpxRecordState)
        }.collectWhileResumedIn(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        /* The action bar isn't managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = getString(R.string.recording_frgmt_title)
        }

        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recordListView.setListener(object : RecordListView.RecordListViewListener {
            override fun onRequestShareRecording(dataList: List<RecordingData>) {
                val activity = activity ?: return
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
            }

            override fun onRequestChooseMap() {
                val fragmentActivity = activity
                if (fragmentActivity != null) {
                    val dialog = MapSelectionForImport()
                    dialog.show(fragmentActivity.supportFragmentManager, "MapSelectionForImport")
                }
            }

            override fun onRequestEditRecording(data: RecordingData) {
                val fragmentActivity = activity
                if (fragmentActivity != null) {
                    val editFieldDialog = TrackFileNameEdit.newInstance(getString(R.string.track_file_name_change), data.name)
                    editFieldDialog.show(fragmentActivity.supportFragmentManager, "EditFieldDialog" + data.name)
                }
            }

            override fun onRequestShowElevationGraph(data: RecordingData) {
                statViewModel.onRequestShowElevation(data)
                findNavController().navigate(R.id.action_recordFragment_to_elevationFragment)
            }

            override fun onRequestDeleteRecordings(dataList: List<RecordingData>) {
                statViewModel.onRequestDeleteRecordings(dataList)
            }

            override fun onSelectionChanged(dataList: List<RecordingData>) {
                viewModel.setSelectedRecordings(dataList)
            }

            override fun onImportFiles() {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

                /* Search for all documents available via installed storage providers */
                intent.type = "*/*"
                importRecordingsLauncher.launch(intent)
            }
        })
    }

    override fun onStart() {
        super.onStart()

        recordingData?.value?.let {
            updateRecordingData(it)
        }
    }

    private fun onGpxImported(event: TrackImporter.GpxImportResult) {
        when (event) {
            is TrackImporter.GpxImportResult.GpxImportOk ->
                /* Tell the user that the track will be shortly available in the map */
                Snackbar.make(binding.root, R.string.track_is_being_added, Snackbar.LENGTH_LONG).show()
            is TrackImporter.GpxImportResult.GpxImportError ->
                /* Tell the user that an error occurred */
                Snackbar.make(binding.root, R.string.track_add_error, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun updateRecordingData(data: List<RecordingData>) {
        binding.recordListView.setRecordingData(data)
    }

    private fun dispatchGpxRecordServiceStatus(gpxRecordState: GpxRecordState) {
        when (gpxRecordState) {
            GpxRecordState.STOPPED -> binding.statusView.onServiceStopped()
            GpxRecordState.STARTED, GpxRecordState.RESUMED -> binding.statusView.onServiceStarted()
            GpxRecordState.PAUSED -> binding.statusView.onServicePaused()
        }
    }

    companion object {
        const val TAG = "RecordFragment"
    }
}
