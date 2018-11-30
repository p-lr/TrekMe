package com.peterlaurence.trekme.ui.record.components

import android.content.Context
import android.graphics.Color
import android.support.design.widget.Snackbar
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.ui.record.components.events.MapSelectedForRecord
import com.peterlaurence.trekme.ui.record.components.events.RequestChooseMap
import com.peterlaurence.trekme.ui.record.components.events.RequestEditRecording
import com.peterlaurence.trekme.ui.tools.RecyclerItemClickListener
import com.peterlaurence.trekme.util.gpx.model.Gpx
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.util.*

/**
 * List of recordings.
 *
 * @author peterLaurence on 23/12/17 -- Converted to Kotlin on 30/09/18
 */
class RecordListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : CardView(context, attrs, defStyleAttr) {
    private var isMultiSelectMode = false
    private var selectedRecordings = ArrayList<File>()
    private lateinit var recordingAdapter: RecordingAdapter
    internal val recordingDataList = arrayListOf<RecordingData>()

    init {
        init(context)
    }

    /**
     * A [RecordingData] is just wrapper on the [File] and its corresponding [Gpx] data.
     */
    data class RecordingData(val recording: File, val gpx: Gpx? = null)

    fun setRecordings(recordings: Array<File>) {
        recordingDataList.clear()
        /* For instance, only fill the file attribute, the gpx data will be retrieved later */
        recordingDataList.addAll(recordings.map { RecordingData(it) })

        /* Update the recycle view */
        recordingAdapter.setRecordingsData(recordingDataList)
    }

    /**
     * Once we receive the [Gpx] data for each recording [File], fill the model object.
     */
    fun setGpxForRecording(recordingsToGpx: kotlin.collections.Map<File, Gpx>) {
        /* Re-write the model object */
        recordingDataList.clear()
        for ((file, gpx) in recordingsToGpx) {
            recordingDataList.add(RecordingData(file, gpx))
        }

        /* Update the recycle view */
        recordingAdapter.setRecordingsData(recordingDataList)
    }

    private fun init(context: Context) {
        View.inflate(context, R.layout.record_list_layout, this)

        val ctx = getContext()
        val recyclerView = findViewById<RecyclerView>(R.id.recordings_recycler_id)
        val editNameButton = findViewById<ImageButton>(R.id.edit_recording_button)
        val importButton = findViewById<ImageButton>(R.id.import_track_button)
        val deleteRecordingButton = findViewById<ImageButton>(R.id.delete_recording_button)

        editNameButton.isEnabled = false
        editNameButton.setOnClickListener {
            if (selectedRecordings.size == 1) {
                val recording = selectedRecordings[0]
                EventBus.getDefault().post(RequestEditRecording(recording))
            }
        }

        importButton.isEnabled = false
        importButton.setOnClickListener { EventBus.getDefault().post(RequestChooseMap()) }

        deleteRecordingButton.setOnClickListener {
            var success = true
            for (file in selectedRecordings) {
                if (file.exists()) {
                    if (file.delete()) {
                        recordingDataList.removeAll { it.recording == file }
                    } else {
                        success = false
                    }
                }
            }
            recordingAdapter.notifyDataSetChanged()

            /* Alert the user that some files could not be deleted */
            if (!success) {
                val snackbar = Snackbar.make(rootView, R.string.files_could_not_be_deleted,
                        Snackbar.LENGTH_SHORT)
                snackbar.show()
            }
        }

        val llm = LinearLayoutManager(ctx)
        recyclerView.layoutManager = llm

        recordingAdapter = RecordingAdapter(recordingDataList, selectedRecordings)
        recyclerView.adapter = recordingAdapter

        recyclerView.addOnItemTouchListener(RecyclerItemClickListener(this.context,
                recyclerView, object : RecyclerItemClickListener.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                if (isMultiSelectMode) {
                    multiSelect(position)

                    recordingAdapter.setSelectedRecordings(selectedRecordings)
                    recordingAdapter.notifyItemChanged(position)
                } else {
                    singleSelect(position)
                    editNameButton.isEnabled = true
                    editNameButton.drawable.setTint(resources.getColor(R.color.colorAccent, null))

                    importButton.isEnabled = true
                    importButton.drawable.setTint(resources.getColor(R.color.colorAccent, null))

                    recordingAdapter.setSelectedRecordings(selectedRecordings)
                    recordingAdapter.notifyDataSetChanged()
                }
            }

            override fun onItemLongClick(view: View, position: Int) {
                selectedRecordings = ArrayList()
                if (!isMultiSelectMode) {
                    isMultiSelectMode = true
                    editNameButton.isEnabled = false
                    editNameButton.drawable.setTint(Color.GRAY)
                    importButton.isEnabled = false
                    importButton.drawable.setTint(Color.GRAY)
                    deleteRecordingButton.visibility = View.VISIBLE
                    multiSelect(position)
                    recordingAdapter.setSelectedRecordings(selectedRecordings)
                    recordingAdapter.notifyDataSetChanged()
                } else {
                    isMultiSelectMode = false
                    deleteRecordingButton.visibility = View.GONE
                    recordingAdapter.setSelectedRecordings(selectedRecordings)
                    recordingAdapter.notifyDataSetChanged()
                }
            }
        }))
    }

    private fun multiSelect(position: Int) {
        val recording = recordingDataList[position].recording
        if (selectedRecordings.contains(recording)) {
            selectedRecordings.remove(recording)
        } else {
            selectedRecordings.add(recording)
        }
    }

    private fun singleSelect(position: Int) {
        val recording = recordingDataList[position].recording
        selectedRecordings.clear()
        selectedRecordings.add(recording)
    }

    @Subscribe
    fun onMapSelectedForRecord(event: MapSelectedForRecord) {
        val map = MapLoader.getInstance().getMap(event.mapId)
        val recording = selectedRecordings[0]

        TrackImporter.importTrackFile(recording, null, map!!)

        /* Tell the user that the track will be shortly available in the map */
        val snackbar = Snackbar.make(rootView, R.string.track_is_being_added, Snackbar.LENGTH_LONG)
        snackbar.show()
    }
}
