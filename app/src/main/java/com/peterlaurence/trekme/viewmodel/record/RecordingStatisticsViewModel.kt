package com.peterlaurence.trekme.viewmodel.record

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.core.track.TrackStatCalculator
import com.peterlaurence.trekme.core.track.TrackTools
import com.peterlaurence.trekme.core.track.hpFilter
import com.peterlaurence.trekme.service.event.GpxFileWriteEvent
import com.peterlaurence.trekme.ui.record.components.events.RecordingDeletionFailed
import com.peterlaurence.trekme.ui.record.components.events.RecordingNameChangeEvent
import com.peterlaurence.trekme.ui.record.components.events.RequestDeleteRecordings
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.gpx.GPXParser
import com.peterlaurence.trekme.util.gpx.GPXWriter
import com.peterlaurence.trekme.util.gpx.model.Gpx
import com.peterlaurence.trekme.util.gpx.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap


/**
 * Encapsulates the logic of dealing with recordings as [File]s, by converting them to [Gpx] and
 * computing statistics. Those statistics are bundled inside the [Gpx] instances (inside each
 * [Track]).
 * It exposes a [recordings] [LiveData] so that fragments can observe changes.
 * It also responds to some events coming from UI components, such as [GpxFileWriteEvent] and
 * [RecordingNameChangeEvent] to trigger proper update of [recordings].
 */
class RecordingStatisticsViewModel : ViewModel() {

    private val recordingData: MutableLiveData<List<RecordingData>> by lazy {
        MutableLiveData<List<RecordingData>>().apply {
            /* The first emission won't include Gpx instances */
            postValue(recordings.map {
                RecordingData(it)
            })

            /* Immediately request that Gpx instances are created along with statistics */
            updateRecordingData()
        }
    }

    private val recordings: List<File>
        get() = TrackImporter.recordings?.toMutableList() ?: listOf()

    private val recordingsToGpx: MutableMap<File, Gpx> = ConcurrentHashMap()

    fun getRecordingData(): LiveData<List<RecordingData>> {
        return recordingData
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGpxFileWriteEvent(event: GpxFileWriteEvent) {
        updateRecordingData()
    }

    @Subscribe
    fun onRecordingNameChangeEvent(event: RecordingNameChangeEvent) {
        for (recording in recordingsToGpx.keys) {
            if (FileUtils.getFileNameWithoutExtention(recording) == event.initialValue) {
                TrackTools.renameTrack(recording, event.newValue)
                updateRecordingData()
                break
            }
        }
    }

    @Subscribe
    fun onRequestDeleteRecordings(event: RequestDeleteRecordings) {
        var success = true
        event.recordingList.forEach {
            if (it.exists()) {
                if (!it.delete()) {
                    success = false
                }
            }
        }

        if (!success) {
            EventBus.getDefault().post(RecordingDeletionFailed())
        }

        updateRecordingData()
    }

    private fun updateRecordingData() = viewModelScope.launch {
        val data = withContext(Dispatchers.Default) {
            computeStatistics()
        }.map {
            RecordingData(it.key, it.value)
        }

        recordingData.postValue(data)
    }

    /**
     * The user may have imported a regular gpx file (so it doesn't have any statistics).
     * In this call, we must have that each gpx file already been parsed, and the
     * [recordingsToGpx] Map should br up to date.
     * Hence, [updateRecordingsToGpxMap] is called first.
     *
     * Then, we compute the statistics for the first track.
     * If the [GPXParser] read statistics for this track, we check is there is any difference
     * (because the statistics calculation is subjected to be adjusted frequently), we update the
     * gpx file.
     *
     * @return a non modifiable Map<File, Gpx>. The statistics are bundled inside the [Gpx] objects.
     */
    private fun computeStatistics(): Map<File, Gpx> {
        /* Update internals */
        updateRecordingsToGpxMap()

        /* Then compute the statistics */
        recordingsToGpx.forEach {
            val statCalculator = TrackStatCalculator()
            it.value.tracks.firstOrNull()?.let { track ->
                track.trackSegments.forEach { trackSegment ->
                    trackSegment.hpFilter()
                    statCalculator.addTrackPointList(trackSegment.trackPoints)
                }

                val updatedStatistics = statCalculator.getStatistics()
                if (track.statistics != null && track.statistics != updatedStatistics) {
                    /* Track statistics have changed, update the file */
                    track.statistics = updatedStatistics
                    val fos = FileOutputStream(it.key)
                    GPXWriter.write(it.value, fos)
                }
                track.statistics = updatedStatistics
            }
        }

        return recordingsToGpx.toMap()
    }

    /**
     * In the context of this call, new recordings have been added, or this is the first time
     * this function is called in the lifecycle of the  app.
     * The list of recordings, [recordings], is considered up to date. The map between each
     * recording and its corresponding parsed object, [recordingsToGpx], needs to be updated.
     * The first call parses all recordings. Subsequent calls only parse new files.
     * This is a blocking call, so it should be called inside a coroutine.
     */
    private fun updateRecordingsToGpxMap(): Map<File, Gpx> {
        if (recordingsToGpx.isEmpty()) {
            recordings.forEach {
                try {
                    val gpx = GPXParser.parse(FileInputStream(it))
                    recordingsToGpx[it] = gpx
                } catch (e: Exception) {
                    Log.e(TAG, "The file ${it.name} was parsed with an error")
                }
            }
        } else {
            recordings.filter { !recordingsToGpx.keys.contains(it) }.forEach {
                try {
                    val gpx = GPXParser.parse(FileInputStream(it))
                    recordingsToGpx[it] = gpx
                } catch (e: Exception) {
                    Log.e(TAG, "The file ${it.name} was parsed with an error")
                }
            }
            recordingsToGpx.keys.filter { !(recordings.contains(it)) }.forEach {
                recordingsToGpx.remove(it)
            }
        }
        return recordingsToGpx.toMap()
    }

    init {
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        super.onCleared()

        EventBus.getDefault().unregister(this)
    }
}

private const val TAG = "RecordingStatisticsVM"

/**
 * A [RecordingData] is just wrapper on the [File] and its corresponding [Gpx] data.
 */
data class RecordingData(val recording: File, val gpx: Gpx? = null)