package com.peterlaurence.trekme.ui.record


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.fileprovider.TrekmeFilesProvider
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.databinding.FragmentRecordBinding
import com.peterlaurence.trekme.service.LocationService
import com.peterlaurence.trekme.ui.dialogs.EditFieldDialog
import com.peterlaurence.trekme.ui.dialogs.MapChoiceDialog
import com.peterlaurence.trekme.ui.events.RecordGpxStopEvent
import com.peterlaurence.trekme.ui.record.components.RecordListView
import com.peterlaurence.trekme.ui.record.components.events.RecordingNameChangeEvent
import com.peterlaurence.trekme.ui.record.components.events.RequestStartEvent
import com.peterlaurence.trekme.ui.record.components.events.RequestStopEvent
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.viewmodel.LocationServiceViewModel
import com.peterlaurence.trekme.viewmodel.record.RecordViewModel
import com.peterlaurence.trekme.viewmodel.record.RecordingData
import com.peterlaurence.trekme.viewmodel.record.RecordingStatisticsViewModel
import kotlinx.android.synthetic.main.fragment_record.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File

/**
 * Holds controls and displays information about the [LocationService].
 * Displays the list of records (gpx files) along their statistics.
 *
 * @author peterLaurence -- converted to Kotlin on 01/11/18
 */
class RecordFragment : Fragment() {
    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    val viewModel: RecordViewModel by activityViewModels()
    private lateinit var recordingData: LiveData<List<RecordingData>>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val statViewModel: RecordingStatisticsViewModel by activityViewModels()
        recordingData = statViewModel.getRecordingData()
        recordingData.observe(viewLifecycleOwner, Observer {
            it?.let { data ->
                updateRecordingData(data)
            }
        })

        /**
         * Observe the changes in the Location service status, and update child views accordingly.
         */
        val locationServiceViewModel: LocationServiceViewModel by activityViewModels()
        locationServiceViewModel.getStatus().observe(
                viewLifecycleOwner, Observer {
            it?.let { isActive ->
                dispatchLocationServiceStatus(isActive)
            }
        })

        recordListView.setListener(object : RecordListView.RecordListViewListener {
            override fun onRequestShareRecording(recordings: List<File>) {
                val activity = activity ?: return
                val intentBuilder = ShareCompat.IntentBuilder.from(activity)
                        .setType("text/plain")
                recordings.forEach {
                    try {
                        val uri = TrekmeFilesProvider.generateUri(it)
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
                    val dialog = MapChoiceDialog()
                    dialog.show(fragmentActivity.supportFragmentManager, "MapChoiceDialog")
                }
            }

            override fun onRequestEditRecording(recording: File) {
                val fragmentActivity = activity
                if (fragmentActivity != null) {
                    val recordingName = FileUtils.getFileNameWithoutExtention(recording)
                    val eventBack = RecordingNameChangeEvent("", "")
                    val editFieldDialog = EditFieldDialog.newInstance(getString(R.string.track_file_name_change), recordingName, eventBack)
                    editFieldDialog.show(fragmentActivity.supportFragmentManager, "EditFieldDialog" + recording.name)
                }
            }

            override fun onRequestDeleteRecordings(recordings: List<File>) {
                statViewModel.onRequestDeleteRecordings(recordings)
            }

            override fun onSelectionChanged(recordings: List<File>) {
                viewModel.setSelectedRecordings(recordings)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        EventBus.getDefault().register(binding.recordListView)

        recordingData.value?.let {
            updateRecordingData(it)
        }
    }

    override fun onStop() {
        EventBus.getDefault().unregister(binding.recordListView)
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe
    fun onRequestStartEvent(event: RequestStartEvent) {
        val intent = Intent(requireActivity().baseContext, LocationService::class.java)
        requireActivity().startService(intent)
    }

    @Subscribe
    fun onRequestStopEvent(event: RequestStopEvent) {
        EventBus.getDefault().post(RecordGpxStopEvent())
    }

    @Subscribe
    fun onGpxImported(event: TrackImporter.GpxParseResult) {
        /* Tell the user that the track will be shortly available in the map */
        Snackbar.make(binding.root, R.string.track_is_being_added, Snackbar.LENGTH_LONG).show()
    }

    private fun updateRecordingData(data: List<RecordingData>) {
        binding.recordListView.setRecordingData(data)
    }

    private fun dispatchLocationServiceStatus(isActive: Boolean) {
        if (isActive) {
            binding.statusView.onServiceStarted()
            binding.actionsView.onServiceStarted()
        } else {
            binding.statusView.onServiceStopped()
            binding.actionsView.onServiceStopped()
        }
    }
}
