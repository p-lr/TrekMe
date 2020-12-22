@file:Suppress("BlockingMethodInNonBlockingContext")

package com.peterlaurence.trekme.viewmodel.record

import android.net.Uri
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.fileprovider.TrekmeFilesProvider
import com.peterlaurence.trekme.core.track.*
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.repositories.recording.GpxRepository
import com.peterlaurence.trekme.service.event.GpxFileWriteEvent
import com.peterlaurence.trekme.ui.record.components.events.RecordingNameChangeEvent
import com.peterlaurence.trekme.ui.record.events.RecordEventBus
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.gpx.model.Gpx
import com.peterlaurence.trekme.util.gpx.parseGpx
import com.peterlaurence.trekme.util.gpx.parseGpxSafely
import com.peterlaurence.trekme.util.stackTraceToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap


/**
 * Associates each recordings (as [File]s) with [RecordingData]s, which contain basic properties and
 * track statistics. Those statistics come from dynamically computed [Gpx] instances. However,
 * [Gpx] objects are potentially heavy. This is the reason why we don't store them, and use
 * lightweight [RecordingData].
 *
 * This view-model exposes a [recordings] livedata so that fragments can observe changes.
 * It also responds to some events coming from UI components, such as [GpxFileWriteEvent] and
 * [RecordingNameChangeEvent] to trigger proper update of [recordings].
 *
 * @author P.Laurence on 21/04/19
 */
class RecordingStatisticsViewModel @ViewModelInject constructor(
        private val gpxRecordEvents: GpxRecordEvents,
        private val gpxRepository: GpxRepository,
        private val eventBus: RecordEventBus
) : ViewModel() {

    private val recordingData: MutableLiveData<List<RecordingData>> by lazy {
        MutableLiveData<List<RecordingData>>().apply {
            /* The first emission won't include Gpx instances */
            postValue(recordings.map {
                makeRecordingData(it)
            })

            /* Immediately request that Gpx instances are created along with statistics */
            initDataSet()
        }
    }

    private val recordings: List<File>
        get() = gpxRepository.recordings?.toList() ?: listOf()

    private val recordingsToData: MutableMap<File, RecordingData> = ConcurrentHashMap()

    init {
        viewModelScope.launch {
            gpxRecordEvents.gpxFileWriteEvent.collect {
                addOneRecording(it.gpxFile, it.gpx)
            }
        }

        viewModelScope.launch {
            eventBus.recordingNameChangeEvent.collect {
                onRecordingNameChangeEvent(it)
            }
        }
    }

    fun getRecordingData(): LiveData<List<RecordingData>> {
        return recordingData
    }

    fun getRecordingUri(recordingData: RecordingData): Uri? {
        val gpxFile = getGpxFileForId(recordingData.id)
        return if (gpxFile != null) TrekmeFilesProvider.generateUri(gpxFile) else null
    }

    private fun getGpxFileForId(id: Int): File? {
        return recordings.firstOrNull { it.id() == id }
    }

    /**
     * Remove the existing file matching by name, then add the new file along with the new
     * [RecordingData] instance.
     */
    private suspend fun onRecordingNameChangeEvent(event: RecordingNameChangeEvent) {
        with(recordingsToData.iterator()) {
            forEach {
                val gpxFile = it.key
                if (FileUtils.getFileNameWithoutExtention(gpxFile) == event.initialValue) {
                    val oldData = recordingsToData[gpxFile] ?: return
                    val newFile = File(gpxFile.parent, event.newValue + "." + FileUtils.getFileExtension(gpxFile))
                    val newData = oldData.copy(name = event.newValue)
                    val success = withContext(Dispatchers.IO) {
                        TrackTools.renameGpxFile(gpxFile, newFile)
                    }
                    if (success) {
                        remove()
                        recordingsToData[newFile] = newData
                    }
                    updateLiveData()
                    return
                }
            }
        }
    }

    fun onRequestDeleteRecordings(recordingDataList: List<RecordingData>) = viewModelScope.launch {
        val ids = recordingDataList.map { it.id }
        val recordingsToDelete = recordings.filter { it.id() in ids }
        var success = true
        supervisorScope {
            with(recordingsToData.iterator()) {
                forEach {
                    val file = it.key
                    if (file in recordingsToDelete) {
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
            eventBus.postRecordingDeletionFailed()
        }
    }

    fun onRequestShowElevation(recordingData: RecordingData) = viewModelScope.launch {
        /* If we already computed elevation data for this same gpx file, no need to continue */
        val gpxForElevation = gpxRepository.gpxForElevation.replayCache.firstOrNull()
        if (gpxForElevation != null && gpxForElevation.id == recordingData.id) return@launch

        /* Notify the repo that we're about to submit new data, invalidating the existing one */
        gpxRepository.resetGpxForElevation()

        val (gpx, gpxFile) = withContext(Dispatchers.IO) {
            val gpxFile = getGpxFileForId(recordingData.id)
            Pair(parseGpxSafely(FileInputStream(gpxFile)), gpxFile)
        }

        if (gpx != null && gpxFile != null) {
            gpxRepository.setGpxForElevation(gpx, gpxFile.id())
        }
    }

    private fun addOneRecording(gpxFile: File, gpx: Gpx) = viewModelScope.launch {
        /* Add the file if not already present */
        if (!recordingsToData.containsKey(gpxFile)) {
            recordingsToData[gpxFile] = makeRecordingData(gpxFile, gpx)
        }

        setTrackStatistics(gpx)
        updateLiveData()
    }

    private fun updateLiveData() {
        /* Transform a defensive copy of the original Map - the view uses DiffUtils, which performs
         * background calculations on the given data structure. */
        val data = recordingsToData.toMap().values.sortedByDescending {
            it.time ?: -1
        }

        recordingData.postValue(data)
    }

    /**
     * Compute all [Gpx] and statistics, then notify views.
     * It's invoked once, on first acquisition of the [LiveData].
     */
    private fun initDataSet() = viewModelScope.launch {
        updateRecordingsToGpxMap()
        updateLiveData()
    }

    private suspend fun setTrackStatistics(gpx: Gpx) = withContext(Dispatchers.Default) {
        gpx.tracks.firstOrNull()?.let { track ->
            val statCalculator = TrackStatCalculator()
            track.trackSegments.forEach { trackSegment ->
                statCalculator.addTrackPointList(trackSegment.trackPoints)
            }

            val updatedStatistics = statCalculator.getStatistics()
            track.statistics = updatedStatistics
        }
    }

    private fun makeRecordingData(gpxFile: File, gpx: Gpx? = null): RecordingData {
        return RecordingData(
                gpxFile.id(),
                FileUtils.getFileNameWithoutExtention(gpxFile),
                gpx?.tracks?.firstOrNull()?.statistics,
                gpx?.metadata?.time
        )
    }

    /**
     * Updates the correspondence between each recording and its corresponding [RecordingData]
     * instance ([recordingsToData]).
     * Concurrently parses all recordings and computing statistics.
     */
    private suspend fun updateRecordingsToGpxMap() = withContext(Dispatchers.Default) {
        suspend fun parseGpxAndComputeStats(file: File): Gpx? {
            return try {
                val gpx = parseGpx(FileInputStream(file))
                setTrackStatistics(gpx)
                recordingsToData[file] = makeRecordingData(file, gpx)
                gpx
            } catch (e: Exception) {
                Log.e(TAG, "The file ${file.name} was parsed with error ${stackTraceToString(e)}")
                null
            }
        }

        /* Update our data structure with controlled concurrency */
        val coreCount = (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(2)
        recordings.asFlow().mapNotNull { file ->
            flow {
                val gpx = if (!recordingsToData.keys.contains(file)) {
                    parseGpxAndComputeStats(file)
                } else null
                emit(gpx)
            }
        }.flattenMerge(coreCount).collect()
    }
}

private const val TAG = "RecordingStatisticsVM"

/**
 * A [RecordingData] is just wrapper on the [File] and its corresponding [TrackStatistics] data and
 * timestamp. The timestamp is used to sort visual elements.
 */
data class RecordingData(val id: Int, val name: String, val statistics: TrackStatistics? = null, val time: Long? = null)