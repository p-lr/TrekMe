package com.peterlaurence.trekme.ui.record


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.core.track.TrackTools
import com.peterlaurence.trekme.service.LocationService
import com.peterlaurence.trekme.service.event.GpxFileWriteEvent
import com.peterlaurence.trekme.service.event.LocationServiceStatus
import com.peterlaurence.trekme.ui.dialogs.EditFieldDialog
import com.peterlaurence.trekme.ui.events.RecordGpxStopEvent
import com.peterlaurence.trekme.ui.record.components.dialogs.MapChoiceDialog
import com.peterlaurence.trekme.ui.record.components.events.*
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.core.fileprovider.TrekmeFilesProvider
import kotlinx.android.synthetic.main.fragment_record.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import kotlin.coroutines.CoroutineContext
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.track.TrackImporter.applyGpxFileToMapAsync

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
    fun onRequestShareRecording(event: RequestShareRecording) {
        val intentBuilder = ShareCompat.IntentBuilder.from(activity)
                .setType("text/plain")
        event.recordings.forEach {
            try {
                val uri = TrekmeFilesProvider.generateUri(it)
                intentBuilder.addStream(uri)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
        intentBuilder.startChooser()
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

    @Subscribe
    fun onImportRecordingEvent(event: RequestImportRecording) = launch {
        /* If unhandled Exceptions need to be caught, it should be done here */
        applyGpxFileToMap(event.file, event.map)
    }

    /**
     * The business logic of parsing a GPX file.
     * It is wrapped in a child [CoroutineScope] because we use an `async` call, which by default
     * defers Exception handling to the calling code. If an unhandled Exception is thrown, it leads
     * to a failure of the parent scope **even if those Exceptions are caught**. We don't want the
     * whole scope of this fragment to fail, hence the child [CoroutineScope].
     *
     * @throws TrackImporter.GpxParseException
     */
    private suspend fun applyGpxFileToMap(file: File, map: Map) = coroutineScope {
        applyGpxFileToMapAsync(file, map).await().let {
            /* Once done, all we want is to post an event */
            EventBus.getDefault().post(it)
        }
    }

    private fun CoroutineScope.updateRecordings() = launch {
        TrackImporter.recordings?.let {
            recordListView.setRecordings(it)
        }

        /* Recording to Gpx conversion
         * Then, ask for the computation of the statistics for all tracks */
        val recordingsToGpx = async(Dispatchers.Default) {
            TrackImporter.computeStatistics()
        }

        recordListView.setGpxForRecording(recordingsToGpx.await())
    }
}
