package com.peterlaurence.trekadvisor.menu.record


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.core.track.TrackImporter
import com.peterlaurence.trekadvisor.core.track.TrackTools
import com.peterlaurence.trekadvisor.menu.dialogs.EditFieldDialog
import com.peterlaurence.trekadvisor.menu.events.RecordGpxStopEvent
import com.peterlaurence.trekadvisor.menu.record.components.dialogs.MapChoiceDialog
import com.peterlaurence.trekadvisor.menu.record.components.events.*
import com.peterlaurence.trekadvisor.service.LocationService
import com.peterlaurence.trekadvisor.service.event.GpxFileWriteEvent
import com.peterlaurence.trekadvisor.service.event.LocationServiceStatus
import com.peterlaurence.trekadvisor.util.FileUtils
import kotlinx.android.synthetic.main.fragment_record.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.coroutines.CoroutineContext

/**
 * Holds controls and displays information about the [LocationService].
 * Also, various coroutines are launched in the context of this fragment to perform asynchronous
 * operations to fetch and update data, etc. They are automatically cancelled when this fragment is
 * stopped, to avoid memory leaks. See [this link](https://kotlinlang.org/docs/reference/coroutines/coroutine-context-and-dispatchers.html#cancellation-via-explicit-job)
 *
 * @author peterLaurence -- converted to Kotlin on 01/11/18
 */
class RecordFragment : Fragment(), CoroutineScope {
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    override fun onStart() {
        super.onStart()
        job = Job()
        EventBus.getDefault().register(this)
        EventBus.getDefault().register(actionsView)
        EventBus.getDefault().register(recordListView)

        updateRecordings()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(actionsView)
        EventBus.getDefault().unregister(recordListView)
        EventBus.getDefault().unregister(this)
        job.cancel()
        super.onStop()
    }

    @Subscribe
    fun onRequestStartEvent(event: RequestStartEvent) {
        val intent = Intent(activity!!.baseContext, LocationService::class.java)
        activity!!.startService(intent)
    }

    @Subscribe
    fun onRequestStopEvent(event: RequestStopEvent) {
        EventBus.getDefault().post(RecordGpxStopEvent())
    }

    @Subscribe
    fun onLocationServiceStatusEvent(event: LocationServiceStatus) {
        if (event.started) {
            statusView.onServiceStarted()
        } else {
            statusView.onServiceStopped()
        }
    }

    /**
     * The [RecordFragment] is only used here to show the dialog.
     */
    @Subscribe
    fun onRequestEditRecording(event: RequestEditRecording) {
        val fragmentActivity = activity
        if (fragmentActivity != null) {
            val recordingName = FileUtils.getFileNameWithoutExtention(event.recording)
            val eventBack = RecordingNameChangeEvent("", "")
            val editFieldDialog = EditFieldDialog.newInstance(getString(R.string.track_file_name_change), recordingName, eventBack)
            editFieldDialog.show(fragmentActivity.supportFragmentManager, "EditFieldDialog" + event.recording.name)
        }
    }

    @Subscribe
    fun onRequestChooseMap(event: RequestChooseMap) {
        val fragmentActivity = activity
        if (fragmentActivity != null) {
            val dialog = MapChoiceDialog()
            dialog.show(fragmentActivity.supportFragmentManager, "MapChoiceDialog")
        }
    }

    @Subscribe
    fun onRecordingNameChangeEvent(event: RecordingNameChangeEvent) {
        for (recording in recordListView.recordingDataList.map { it.recording }) {
            if (FileUtils.getFileNameWithoutExtention(recording) == event.initialValue) {
                TrackTools.renameTrack(recording, event.newValue)
            }
        }
        updateRecordings()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGpxFileWriteEvent(event: GpxFileWriteEvent) {
        updateRecordings()
    }

    private fun CoroutineScope.updateRecordings() = launch {
        TrackImporter.recordings?.let {
            recordListView.setRecordings(it)
        }

        /* Recording to Gpx conversion */
        var recordingsToGpx = launch(Dispatchers.Default) {
            TrackImporter.getRecordingsToGpxMap()
        }
        recordingsToGpx.join()

        /* Then, ask for the computation of the statistics for all tracks */
        recordingsToGpx = async(Dispatchers.Default) {
            TrackImporter.computeStatistics()
        }

        recordListView.setGpxForRecording(recordingsToGpx.await())
    }
}
