package com.peterlaurence.trekadvisor.menu.record.components

import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.core.track.TrackStatistics
import com.peterlaurence.trekadvisor.util.UnitFormatter
import java.io.File
import java.util.*

/**
 * Adapter class for recordings. It holds the view logic associated with the [RecyclerView]
 * defined in the [RecordListView].
 *
 * @author peterLaurence on 27/01/18 -- Converted to Kotlin on 01/10/18
 */
class RecordingAdapter internal constructor(private var recordingDataList: ArrayList<RecordListView.RecordingData>, private var selectedRecordings: ArrayList<File>?) : RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.record_item, parent, false)
        return RecordingViewHolder(v)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.recordingName.text = recordingDataList[position].recording.name

        /* If there is some statistics attached to the first track, show the corresponding view */
        holder.statView.visibility = recordingDataList[position].gpx?.tracks?.firstOrNull()?.statistics?.let {
            holder.statView.setStatistics(it)
            View.VISIBLE
        } ?: View.GONE

        if (selectedRecordings!!.contains(recordingDataList.map { it.recording }[position])) {
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
        return recordingDataList.size
    }

    class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var layout: ConstraintLayout = itemView.findViewById(R.id.record_item_layout)
        var recordingName: TextView = itemView.findViewById(R.id.recording_name_id)
        var statView: ConstraintLayout = itemView.findViewById(R.id.stats_view_holder)
    }

    internal fun setRecordingsData(recordingDataList: ArrayList<RecordListView.RecordingData>) {
        this.recordingDataList = recordingDataList
        notifyDataSetChanged()
    }

    internal fun setSelectedRecordings(selectedRecordings: ArrayList<File>) {
        this.selectedRecordings = selectedRecordings
    }

    private fun ConstraintLayout.setStatistics(stat: TrackStatistics) {
        val distanceText = findViewById<TextView>(R.id.record_item_distance_stat)
        distanceText.text = UnitFormatter.formatDistance(stat.distance)

        val elevationUpStackText = findViewById<TextView>(R.id.record_item_up_stat)
        elevationUpStackText.text = "+".plus(UnitFormatter.formatDistance(stat.elevationUpStack))

        val elevationDownStackText = findViewById<TextView>(R.id.record_item_down_stat)
        elevationDownStackText.text = "-".plus(UnitFormatter.formatDistance(stat.elevationDownStack))
    }
}
