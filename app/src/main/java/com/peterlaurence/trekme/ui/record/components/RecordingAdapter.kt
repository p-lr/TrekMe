package com.peterlaurence.trekme.ui.record.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.track.TrackStatistics
import com.peterlaurence.trekme.util.formatDistance
import com.peterlaurence.trekme.viewmodel.record.RecordingData
import java.io.File
import java.util.*

/**
 * Adapter class for recordings. It holds the view logic associated with the [RecyclerView]
 * defined in the [RecordListView].
 *
 * @author peterLaurence on 27/01/18 -- Converted to Kotlin on 01/10/18
 */
class RecordingAdapter(
        private var selectedRecordings: ArrayList<File>
) : RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {
    private val diffCallback: DiffUtil.ItemCallback<RecordingData> = object : DiffUtil.ItemCallback<RecordingData>() {
        override fun areItemsTheSame(oldItem: RecordingData, newItem: RecordingData): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: RecordingData, newItem: RecordingData): Boolean {
            return false // force re-render because of alternate color background
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    fun setRecordingsData(recordingDataList: List<RecordingData>, cb: (() -> Unit)? = null) {
        differ.submitList(recordingDataList.toList()) { cb?.invoke() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.record_item, parent, false)
        return RecordingViewHolder(v)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val data = differ.currentList[position] ?: return
        holder.recordingName.text = data.recording.name

        /* If there is some statistics attached to the first track, show the corresponding view */
        holder.statView.visibility = data.gpx?.tracks?.firstOrNull()?.statistics?.let {
            holder.statView.setStatistics(it)
            View.VISIBLE
        } ?: View.GONE

        if (selectedRecordings.contains(data.recording)) {
            holder.layout.setBackgroundColor(-0x77de690d)
        } else {
            if (position % 2 == 0) {
                holder.layout.setBackgroundColor(-0x121213)
            } else {
                holder.layout.setBackgroundColor(-0x1)
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var layout: ConstraintLayout = itemView.findViewById(R.id.record_item_layout)
        var recordingName: TextView = itemView.findViewById(R.id.recording_name_id)
        var statView: ConstraintLayout = itemView.findViewById(R.id.stats_view_holder)
    }

    fun setSelectedRecordings(selectedRecordings: ArrayList<File>) {
        this.selectedRecordings = selectedRecordings
    }

    private fun ConstraintLayout.setStatistics(stat: TrackStatistics) {
        val distanceText = findViewById<TextView>(R.id.record_item_distance_stat)
        distanceText.text = formatDistance(stat.distance)

        val elevationUpStackText = findViewById<TextView>(R.id.record_item_up_stat)
        elevationUpStackText.text = "+".plus(formatDistance(stat.elevationUpStack))

        val elevationDownStackText = findViewById<TextView>(R.id.record_item_down_stat)
        elevationDownStackText.text = "-".plus(formatDistance(stat.elevationDownStack))
    }
}
