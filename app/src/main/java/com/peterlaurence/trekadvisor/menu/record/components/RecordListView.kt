package com.peterlaurence.trekadvisor.menu.record.components

import android.content.Context
import android.graphics.Color
import android.support.design.widget.Snackbar
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.core.map.Map
import com.peterlaurence.trekadvisor.core.map.gson.RouteGson
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader
import com.peterlaurence.trekadvisor.core.track.TrackImporter
import com.peterlaurence.trekadvisor.core.track.TrackTools
import com.peterlaurence.trekadvisor.menu.record.components.events.MapSelectedForRecord
import com.peterlaurence.trekadvisor.menu.record.components.events.RecordingNameChangeEvent
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestChooseMap
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestEditRecording
import com.peterlaurence.trekadvisor.menu.tools.RecyclerItemClickListener
import com.peterlaurence.trekadvisor.service.event.GpxFileWriteEvent
import com.peterlaurence.trekadvisor.util.FileUtils
import com.peterlaurence.trekadvisor.util.gpx.model.Gpx
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.coroutines.experimental.asReference
import java.io.File
import java.util.*

/**
 * List of recordings.
 *
 * @author peterLaurence on 23/12/17 -- Converted to Kotlin on 30/09/18
 */
class RecordListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : CardView(context, attrs, defStyleAttr) {
    private var mIsMultiSelectMode = false
    private var mSelectedRecordings = ArrayList<File>()
    private val mRecordings = ArrayList<File>()
    private var mRecordingAdapter: RecordingAdapter? = null
    private var job: Job? = null

    init {
        updateRecordings()
        init(context, attrs)
    }

    private fun updateRecordings() {
        mRecordings.clear()
        val recordings = TrackImporter.recordings
        if (recordings != null) {
            mRecordings.addAll(Arrays.asList(*recordings))
        }

        /* Recording to Gpx conversion */
        val ref = this.asReference()
        job = launch(UI) {
            val recordingsToGpx = async {
                TrackImporter.getRecordingsToGpxMap()
            }
            ref().setGpxForRecording(recordingsToGpx.await())
        }
    }

    private fun setGpxForRecording(recordingsToGpx: kotlin.collections.Map<File, Gpx>) {
        println("update UI GPX : ${recordingsToGpx.size}")
        //TODO: display statistics
    }

    fun cancelPendingJobs() {
        val job = job
        job?.let {
            if (it.isActive) {
                it.cancel()
            }
        }
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        View.inflate(context, R.layout.record_list_layout, this)

        val ctx = getContext()
        val recyclerView = findViewById<RecyclerView>(R.id.recordings_recycler_id)
        val editNameButton = findViewById<ImageButton>(R.id.edit_recording_button)
        val importButton = findViewById<ImageButton>(R.id.import_track_button)
        val deleteRecordingButton = findViewById<ImageButton>(R.id.delete_recording_button)

        editNameButton.isEnabled = false
        editNameButton.setOnClickListener { v ->
            if (mSelectedRecordings.size == 1) {
                val recording = mSelectedRecordings[0]
                EventBus.getDefault().post(RequestEditRecording(recording))
            }
        }

        importButton.isEnabled = false
        importButton.setOnClickListener { v -> EventBus.getDefault().post(RequestChooseMap()) }

        deleteRecordingButton.setOnClickListener { v ->
            var success = true
            for (file in mSelectedRecordings) {
                if (file.exists()) {
                    if (file.delete()) {
                        mRecordings.remove(file)
                    } else {
                        success = false
                    }
                }
            }
            mRecordingAdapter!!.notifyDataSetChanged()

            /* Alert the user that some files could not be deleted */
            if (!success) {
                val snackbar = Snackbar.make(rootView, R.string.files_could_not_be_deleted,
                        Snackbar.LENGTH_SHORT)
                snackbar.show()
            }
        }

        val llm = LinearLayoutManager(ctx)
        recyclerView.layoutManager = llm

        mRecordingAdapter = RecordingAdapter(mRecordings, mSelectedRecordings)
        recyclerView.adapter = mRecordingAdapter

        recyclerView.addOnItemTouchListener(RecyclerItemClickListener(this.context,
                recyclerView, object : RecyclerItemClickListener.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                if (mIsMultiSelectMode) {
                    multiSelect(position)

                    mRecordingAdapter!!.setSelectedRecordings(mSelectedRecordings)
                    mRecordingAdapter!!.notifyItemChanged(position)
                } else {
                    singleSelect(position)
                    editNameButton.isEnabled = true
                    editNameButton.drawable.setTint(resources.getColor(R.color.colorAccent, null))

                    importButton.isEnabled = true
                    importButton.drawable.setTint(resources.getColor(R.color.colorAccent, null))

                    mRecordingAdapter!!.setSelectedRecordings(mSelectedRecordings)
                    mRecordingAdapter!!.notifyDataSetChanged()
                }
            }

            override fun onItemLongClick(view: View, position: Int) {
                mSelectedRecordings = ArrayList()
                if (!mIsMultiSelectMode) {
                    mIsMultiSelectMode = true
                    editNameButton.isEnabled = false
                    editNameButton.drawable.setTint(Color.GRAY)
                    importButton.isEnabled = false
                    importButton.drawable.setTint(Color.GRAY)
                    deleteRecordingButton.visibility = View.VISIBLE
                    multiSelect(position)
                    mRecordingAdapter!!.setSelectedRecordings(mSelectedRecordings)
                    mRecordingAdapter!!.notifyDataSetChanged()
                } else {
                    mIsMultiSelectMode = false
                    deleteRecordingButton.visibility = View.GONE
                    mRecordingAdapter!!.setSelectedRecordings(mSelectedRecordings)
                    mRecordingAdapter!!.notifyDataSetChanged()
                }
            }
        }))
    }

    private fun multiSelect(position: Int) {
        val recording = mRecordings[position]
        if (mSelectedRecordings.contains(recording)) {
            mSelectedRecordings.remove(recording)
        } else {
            mSelectedRecordings.add(recording)
        }
    }

    private fun singleSelect(position: Int) {
        val recording = mRecordings[position]
        mSelectedRecordings.clear()
        mSelectedRecordings.add(recording)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGpxFileWriteEvent(event: GpxFileWriteEvent) {
        updateRecordings()
        mRecordingAdapter!!.notifyDataSetChanged()
    }

    @Subscribe
    fun onRecordingNameChangeEvent(event: RecordingNameChangeEvent) {
        for (recording in mRecordings) {
            if (FileUtils.getFileNameWithoutExtention(recording) == event.initialValue) {
                TrackTools.renameTrack(recording, event.newValue)
            }
        }
        updateRecordings()
        mRecordingAdapter!!.setRecordings(mRecordings)
    }

    @Subscribe
    fun onMapSelectedForRecord(event: MapSelectedForRecord) {
        val map = MapLoader.getInstance().getMap(event.mapId)
        val listener = object : TrackImporter.TrackFileParsedListener {
            override fun onTrackFileParsed(map: Map, routeList: List<RouteGson.Route>) {
                TrackTools.updateRouteList(map, routeList)
                MapLoader.getInstance().saveRoutes(map)
            }

            override fun onError(message: String) {
                // TODO : log this error
            }
        }
        val recording = mSelectedRecordings[0]

        TrackImporter.importTrackFile(recording, listener, map!!)

        /* Tell the user that the track will be shortly available in the map */
        val snackbar = Snackbar.make(rootView, R.string.track_is_being_added, Snackbar.LENGTH_LONG)
        snackbar.show()
    }
}
