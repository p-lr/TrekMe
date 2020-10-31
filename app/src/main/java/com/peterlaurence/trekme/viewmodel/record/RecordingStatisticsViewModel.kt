package com.peterlaurence.trekme.viewmodel.record

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.core.track.TrackStatCalculator
import com.peterlaurence.trekme.core.track.TrackTools
import com.peterlaurence.trekme.core.track.hpFilter
import com.peterlaurence.trekme.repositories.recording.GpxRecordRepository
import com.peterlaurence.trekme.service.event.GpxFileWriteEvent
import com.peterlaurence.trekme.ui.record.components.events.RecordingDeletionFailed
import com.peterlaurence.trekme.ui.record.components.events.RecordingNameChangeEvent
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.gpx.model.Gpx
import com.peterlaurence.trekme.util.gpx.model.Track
import com.peterlaurence.trekme.util.gpx.parseGpx
import com.peterlaurence.trekme.util.stackTraceToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap


/**
 * Encapsulates the logic of dealing with recordings as [File]s, by converting them to [Gpx] and
 * computing statistics. Those statistics are bundled inside the [Gpx] instances (inside each
 * [Track]).
 * It exposes a [recordings] [LiveData] so that fragments can observe changes.
 * It also responds to some events coming from UI components, such as [GpxFileWriteEvent] and
 * [RecordingNameChangeEvent] to trigger proper update of [recordings].
 */
class RecordingStatisticsViewModel @ViewModelInject constructor(
        private val trackImporter: TrackImporter,
        private val gpxRecordRepository: GpxRecordRepository
) : ViewModel() {

    private val recordingData: MutableLiveData<List<RecordingData>> by lazy {
        MutableLiveData<List<RecordingData>>().apply {
            /* The first emission won't include Gpx instances */
            postValue(recordings.map {
                RecordingData(it)
            })

            /* Immediately request that Gpx instances are created along with statistics */
            initDataSet()
        }
    }

    private val recordings: List<File>
        get() = trackImporter.recordings?.toMutableList() ?: listOf()

    private val recordingsToGpx: MutableMap<File, Gpx> = ConcurrentHashMap()

    init {
        viewModelScope.launch {
            gpxRecordRepository.gpxFileWriteEvent.collect {
                addOneRecording(it.gpxFile, it.gpx)
            }
        }
    }

    fun getRecordingData(): LiveData<List<RecordingData>> {
        return recordingData
    }

    /**
     * Remove the existing file matching by name, then add the new file keeping the existing
     * [Gpx] instance.
     */
    @Subscribe
    fun onRecordingNameChangeEvent(event: RecordingNameChangeEvent) = viewModelScope.launch {
        with(recordingsToGpx.iterator()) {
            forEach {
                val gpxFile = it.key
                if (FileUtils.getFileNameWithoutExtention(gpxFile) == event.initialValue) {
                    val gpx = recordingsToGpx[gpxFile] ?: return@launch
                    val newFile = File(gpxFile.parent, event.newValue + "." + FileUtils.getFileExtension(gpxFile))
                    val success = withContext(Dispatchers.IO) {
                        TrackTools.renameGpxFile(gpxFile, newFile)
                    }
                    if (success) {
                        remove()
                        recordingsToGpx[newFile] = gpx
                    }
                    updateLiveData()
                    return@launch
                }
            }
        }
    }

    fun onRequestDeleteRecordings(recordings: List<File>) = viewModelScope.launch {
        var success = true
        supervisorScope {
            with(recordingsToGpx.iterator()) {
                forEach {
                    val file = it.key
                    if (file in recordings) {
                        launch(Dispatchers.IO) {
                            runCatching {
                                if (file.exists()) {
                                    if (!file.delete()) success = false
                                }
                            }.onFailure {
                                success = false
                            }
                        }
                        /* Immediately remove the element, even if the real removal is pending */
                        remove()
                    }
                }
            }

            updateLiveData()
        }

        /* If only one removal failed, notify the user */
        if (!success) {
            EventBus.getDefault().post(RecordingDeletionFailed())
        }
    }

    private fun addOneRecording(gpxFile: File, gpx: Gpx) = viewModelScope.launch {
        /* Add the file if not already present */
        if (!recordingsToGpx.containsKey(gpxFile)) {
            recordingsToGpx[gpxFile] = gpx
        }

        setTrackStatistics(gpx)
        updateLiveData()
    }

    private fun updateLiveData() {
        /* Transform a copy of the original Map */
        val data = recordingsToGpx.toMap().map {
            RecordingData(it.key, it.value)
        }.sortedByDescending {
            it.gpx?.metadata?.time ?: -1
        }

        recordingData.postValue(data)
    }

    /**
     * Compute all [Gpx] and statistics, then notify views.
     * It's invoked once, on first acquisition of the [LiveData].
     */
    private fun initDataSet() = viewModelScope.launch(Dispatchers.Default) {
        computeGpxAndStatistics()
        updateLiveData()
    }

    /**
     * The user may have imported a regular gpx file (so it doesn't have any statistics).
     * In this method, we first ensure that all gpx files have been parsed, and that the
     * [recordingsToGpx] Map is up to date.
     * Therefore, [updateRecordingsToGpxMap] is called first.
     *
     * Then, we compute the statistics for the first track.
     * If the [GPXParser] read statistics for this track, we check is there is any difference
     * (because the statistics calculation is subjected to be adjusted frequently), we update the
     * gpx file.
     *
     * After this method invocation, the statistics are bundled inside the [Gpx] objects.
     */
    private suspend fun computeGpxAndStatistics() {
        /* Update internals */
        updateRecordingsToGpxMap()

        /* Then compute the statistics */
        recordingsToGpx.forEach {
            setTrackStatistics(it.value)
        }
    }

    private suspend fun setTrackStatistics(gpx: Gpx) = withContext(Dispatchers.Default) {
        gpx.tracks.firstOrNull()?.let { track ->
            val statCalculator = TrackStatCalculator()
            track.trackSegments.forEach { trackSegment ->
                trackSegment.hpFilter()
                statCalculator.addTrackPointList(trackSegment.trackPoints)
            }

            val updatedStatistics = statCalculator.getStatistics()
            track.statistics = updatedStatistics
        }
    }

    /**
     * In the context of this call, new recordings have been added, or this is the first time
     * this function is called in the lifecycle of the app.
     * The list of recordings, [recordings], is considered up to date. The correspondence between
     * each recording and its corresponding [Gpx] instance, [recordingsToGpx], needs to be updated.
     * The first call parses all recordings. Subsequent calls only parse new files.
     * This is a blocking call, so it should be invoked from a coroutine dispatched to
     * [Dispatchers.Default].
     */
    private fun updateRecordingsToGpxMap(): Map<File, Gpx> {
        if (recordingsToGpx.isEmpty()) {
            recordings.forEach {
                try {
                    val gpx = parseGpx(FileInputStream(it))
                    recordingsToGpx[it] = gpx
                } catch (e: Exception) {
                    Log.e(TAG, "The file ${it.name} was parsed with error ${stackTraceToString(e)}")
                }
            }
        } else {
            recordings.filter { !recordingsToGpx.keys.contains(it) }.forEach {
                try {
                    val gpx = parseGpx(FileInputStream(it))
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