package com.peterlaurence.trekme.ui.record.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.RecordListLayoutBinding
import com.peterlaurence.trekme.util.RecyclerItemClickListener
import com.peterlaurence.trekme.viewmodel.record.RecordingData
import java.util.*

/**
 * List of recordings. It displays each recordings showing only the file name at first, then adds
 * the statistics.
 *
 * @author P.Laurence on 23/12/17 -- Converted to Kotlin on 30/09/18
 */
class RecordListView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {
    private var isMultiSelectMode = false
    var selectedRecordings = ArrayList<RecordingData>()
        private set

    private var recyclerView: RecyclerView? = null
    private var recordingAdapter: RecordingAdapter? = null
    private val recordingDataList = arrayListOf<RecordingData>()
    private var listener: RecordListViewListener? = null

    init {
        init(context)
    }

    fun setListener(listener: RecordListViewListener) {
        this.listener = listener
    }

    fun setRecordingData(data: List<RecordingData>) {
        /* If previous size is smaller than the new size, scroll to top */
        val scrollToTop = recordingDataList.size < data.size

        /* Remember the new list */
        recordingDataList.clear()
        recordingDataList.addAll(data)

        /* Update the recycle view */
        recordingAdapter?.setRecordingsData(recordingDataList) {
            if (scrollToTop) {
                recyclerView?.scrollToPosition(0)
            }
        }
    }

    private fun init(context: Context) {
        val b = RecordListLayoutBinding.inflate(LayoutInflater.from(context), this, true)

        val ctx = getContext()
        val toolbar = b.recordListToolbar
        recyclerView = b.recordingsRecyclerId
        val editNameButton = b.editRecordingButton
        val importButton = b.importTrackButton
        val shareButton = b.shareTrackButton
        val elevationGraphButton = b.elevationTrackButton
        val deleteRecordingButton = b.deleteRecordingButton

        toolbar.menu.findItem(R.id.import_new_recordings).setOnMenuItemClickListener {
            listener?.onImportFiles()
            true
        }

        editNameButton.isEnabled = false
        editNameButton.setOnClickListener {
            if (selectedRecordings.size == 1) {
                val recording = selectedRecordings[0]
                listener?.onRequestEditRecording(recording)
            }
        }

        importButton.isEnabled = false
        importButton.setOnClickListener { listener?.onRequestChooseMap() }

        shareButton.isEnabled = false
        shareButton.setOnClickListener {
            if (selectedRecordings.size >= 1) {
                listener?.onRequestShareRecording(selectedRecordings)
            }
        }

        elevationGraphButton.setOnClickListener {
            val selected = selectedRecordings.firstOrNull()
            if (selected != null) listener?.onRequestShowElevationGraph(selected)
        }

        deleteRecordingButton.setOnClickListener {
            listener?.onRequestDeleteRecordings(selectedRecordings)
        }

        val llm = LinearLayoutManager(ctx)
        recyclerView?.layoutManager = llm

        recordingAdapter = RecordingAdapter(selectedRecordings)
        recyclerView?.adapter = recordingAdapter

        recyclerView?.addOnItemTouchListener(
            RecyclerItemClickListener(this.context,
                recyclerView,
                object :
                    RecyclerItemClickListener.OnItemClickListener {
                    val accentTint = resources.getColor(R.color.colorAccent, null)

                    private fun updateButtons() {
                        if (selectedRecordings.isEmpty()) {
                            shareButton.isEnabled = false
                            shareButton.drawable.setTint(Color.GRAY)

                            deleteRecordingButton.visibility = GONE
                        } else {
                            shareButton.isEnabled = true
                            shareButton.drawable.setTint(accentTint)

                            deleteRecordingButton.visibility = VISIBLE
                        }
                    }

                    override fun onItemClick(view: View, position: Int) {
                        if (isMultiSelectMode) {
                            multiSelect(position)

                            recordingAdapter?.setSelectedRecordings(selectedRecordings)
                            recordingAdapter?.notifyItemChanged(position)
                        } else {
                            singleSelect(position)
                            editNameButton.isEnabled = true
                            editNameButton.drawable.setTint(accentTint)

                            importButton.isEnabled = true
                            importButton.drawable.setTint(accentTint)

                            elevationGraphButton.isEnabled = true
                            elevationGraphButton.drawable.setTint(accentTint)

                            recordingAdapter?.setSelectedRecordings(selectedRecordings)
                            recordingAdapter?.notifyDataSetChanged()
                        }

                        updateButtons()
                    }

                    override fun onItemLongClick(view: View, position: Int) {
                        selectedRecordings = ArrayList()
                        if (!isMultiSelectMode) {
                            editNameButton.isEnabled = false
                            editNameButton.drawable.setTint(Color.GRAY)
                            importButton.isEnabled = false
                            importButton.drawable.setTint(Color.GRAY)
                            elevationGraphButton.isEnabled = false
                            elevationGraphButton.drawable.setTint(Color.GRAY)
                            multiSelect(position)
                            recordingAdapter?.setSelectedRecordings(selectedRecordings)
                            recordingAdapter?.notifyDataSetChanged()
                        } else {
                            recordingAdapter?.setSelectedRecordings(selectedRecordings)
                            recordingAdapter?.notifyDataSetChanged()
                        }
                        isMultiSelectMode = !isMultiSelectMode

                        updateButtons()
                    }
                })
        )
    }

    private fun multiSelect(position: Int) {
        val recording = recordingDataList.getOrNull(position) ?: return
        if (selectedRecordings.contains(recording)) {
            selectedRecordings.remove(recording)
        } else {
            selectedRecordings.add(recording)
        }
    }

    private fun singleSelect(position: Int) {
        val recording = recordingDataList.getOrNull(position) ?: return
        selectedRecordings.clear()
        selectedRecordings.add(recording)
    }

    fun onRecordingDeletionFail() {
        /* Alert the user that some files could not be deleted */
        val snackbar = Snackbar.make(rootView, R.string.files_could_not_be_deleted,
                Snackbar.LENGTH_SHORT)
        snackbar.show()
    }

    interface RecordListViewListener {
        fun onRequestShareRecording(dataList: List<RecordingData>)
        fun onRequestChooseMap()
        fun onRequestEditRecording(data: RecordingData)
        fun onRequestShowElevationGraph(data: RecordingData)
        fun onRequestDeleteRecordings(dataList: List<RecordingData>)
        fun onImportFiles()
    }
}
