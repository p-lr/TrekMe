@file:Suppress("BlockingMethodInNonBlockingContext")

package com.peterlaurence.trekme.viewmodel.record

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.events.StandardMessage
import com.peterlaurence.trekme.core.fileprovider.TrekmeFilesProvider
import com.peterlaurence.trekme.core.track.*
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.repositories.recording.GpxRepository
import com.peterlaurence.trekme.service.event.GpxFileWriteEvent
import com.peterlaurence.trekme.ui.record.components.events.RecordingNameChangeEvent
import com.peterlaurence.trekme.ui.record.events.RecordEventBus
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.gpx.model.Gpx
import com.peterlaurence.trekme.util.gpx.model.hasTrustedElevations
import com.peterlaurence.trekme.util.gpx.parseGpx
import com.peterlaurence.trekme.util.gpx.parseGpxSafely
import com.peterlaurence.trekme.util.stackTraceToString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject


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
@HiltViewModel
class RecordingStatisticsViewModel @Inject constructor(
        private val gpxRecordEvents: GpxRecordEvents,
        private val gpxRepository: GpxRepository,
        private val appEventBus: AppEventBus,
        private val eventBus: RecordEventBus,
        private val trekMeContext: TrekMeContext,
        private val app: Application
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

    /**
     * Imports all [Uri]s, and notifies the user when either all imports succeeded, or one of the
     * imports failed.
     */
    fun importRecordings(uriList: List<Uri>) = viewModelScope.launch {
        val contentResolver = app.applicationContext.contentResolver
        var successCnt = 0
        uriList.forEach { uri ->
            if (importRecordingFromUri(uri, contentResolver)) successCnt++
        }
        if (uriList.isNotEmpty() && uriList.size == successCnt) {
            val msg = app.applicationContext.getString(R.string.recording_imported_success, uriList.size)
            appEventBus.postMessage(StandardMessage(msg))
        }
    }

    /**
     * Resolves the given uri to an actual file, and copies it to the app's storage location for
     * recordings. Then, the copied file is parsed to get the corresponding [Gpx] instance along
     * with its statistics. Finally, [recordingData] is updated so the UI can show the imported file.
     */
    private suspend fun importRecordingFromUri(
            uri: Uri, contentResolver: ContentResolver
    ): Boolean = withContext(Dispatchers.IO) {
        var fileName = ""
        runCatching {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            parcelFileDescriptor?.use {
                val fileDescriptor = parcelFileDescriptor.fileDescriptor
                FileInputStream(fileDescriptor).use { fileInputStream ->
                    val name = FileUtils.getFileRealFileNameFromURI(contentResolver, uri)
                            ?: "A track"
                    fileName = name
                    val outputDir = trekMeContext.recordingsDir ?: return@withContext false
                    val file = File(outputDir, name)
                    fileInputStream.copyTo(FileOutputStream(file))
                    val gpx = parseGpx(FileInputStream(file))
                    setTrackStatistics(gpx)
                    addOneRecording(file, gpx)
                }
            }
        }.onFailure {
            val msg = if (fileName.isNotEmpty()) {
                app.applicationContext.getString(R.string.recording_imported_failure, fileName)
            } else {
                app.applicationContext.getString(R.string.recording_imported_failure, "file")
            }
            appEventBus.postMessage(StandardMessage(msg))
        }.isSuccess
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

        withContext(Dispatchers.IO) {
            val gpxFile = getGpxFileForId(recordingData.id)
            val gpx = gpxFile?.let {
                parseGpxSafely(FileInputStream(it))
            }
            if (gpx != null) {
                gpxRepository.setGpxForElevation(gpx, gpxFile)
            }
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

    /**
     * Set track statistics on the first track of the given [Gpx] instance.
     */
    private suspend fun setTrackStatistics(gpx: Gpx) = withContext(Dispatchers.Default) {
        gpx.tracks.firstOrNull()?.let { track ->
            val distanceCalculator = DistanceCalculatorImpl(gpx.hasTrustedElevations())
            val statCalculator = TrackStatCalculator(distanceCalculator)
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