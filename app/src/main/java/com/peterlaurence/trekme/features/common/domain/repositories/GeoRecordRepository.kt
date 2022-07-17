package com.peterlaurence.trekme.features.common.domain.repositories

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.georecord.data.convertGpx
import com.peterlaurence.trekme.core.georecord.domain.interactors.GeoRecordParser
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.track.TrackTools
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.stackTraceToString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Associates each recordings (as [File]s) with [RecordingData]s, which contain basic properties and
 * track statistics. Those statistics come from dynamically computed [GeoRecord] instances. However,
 * [GeoRecord] objects are potentially heavy. This is the reason why we don't store them, and use
 * lightweight [RecordingData].
 *
 * TODO: Pursue the clean arch refactoring by delegating some tasks to the data layer.
 *
 * @since 2022/07/17
 */
@Singleton
class GeoRecordRepository @Inject constructor(
    private val trekMeContext: TrekMeContext,
    private val geoRecordParser: GeoRecordParser,
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val app: Application,
    private val appEventBus: AppEventBus,
    private val gpxRecordEvents: GpxRecordEvents
) {
    private val primaryScope = ProcessLifecycleOwner.get().lifecycleScope


    private val supportedTrackFilesExtensions = arrayOf("gpx", "xml")

    private val supportedFileFilter = filter@{ dir: File, filename: String ->
        /* We only look at files */
        if (File(dir, filename).isDirectory) {
            return@filter false
        }

        supportedTrackFilesExtensions.any { filename.endsWith(".$it") }
    }

    /**
     * Get the list of [File] which extension is in the list of supported extension for track
     * file. Files are searched into the [TrekMeContext.recordingsDir].
     */
    private val recordFiles: Array<File>
        get() = trekMeContext.recordingsDir?.listFiles(supportedFileFilter) ?: emptyArray()

    val recordingDataFlow = MutableStateFlow<List<RecordingData>>(emptyList())

    init {
        primaryScope.launch {
            initRecordingDataFlow()
        }

        primaryScope.launch {
            gpxRecordEvents.gpxFileWriteEvent.collect {
                addOneRecording(it.gpxFile, convertGpx(it.gpx))
            }
        }
    }

    suspend fun deleteRecordings(recordingDataList: List<RecordingData>): Boolean = coroutineScope {
        val files = recordingDataList.map { it.file }
        val recordingsToDelete = recordingDataFlow.value.filter {
            it.file in files
        }
        var success = true

        val byFile = recordingDataFlow.value.associateBy { it.file }.toMutableMap()
        val iter = byFile.iterator()
        iter.forEach { entry ->
            val file = entry.key
            if (file in recordingsToDelete.map { it.file }) {
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
                iter.remove()
            }
        }

        recordingDataFlow.value = byFile.values.toList().mostRecentFirst()

        success
    }

    /**
     * Remove the existing file matching by name, then add the new file along with the new
     * [RecordingData] instance.
     */
    suspend fun renameRecording(oldName: String, newName: String) {
        val existing = recordingDataFlow.value.firstOrNull {
            FileUtils.getFileNameWithoutExtention(it.file) == oldName
        } ?: return

        val newFile = File(existing.file.parent, newName + "." + FileUtils.getFileExtension(existing.file))

        val success = withContext(Dispatchers.IO) {
            TrackTools.renameGpxFile(existing.file, newFile)
        }
        if (success) {
            val newData = existing.copy(file = newFile, name = newName)

            val byFile = recordingDataFlow.value.associateBy { it.file }.toMutableMap()
            byFile.remove(existing.file)
            byFile[newData.file] = newData

            recordingDataFlow.value = byFile.values.toList().mostRecentFirst()
        }
    }

    /**
     * Resolves the given uri to an actual file, and copies it to the app's storage location for
     * recordings. Then, the copied file is parsed to get the corresponding [GeoRecord] instance along
     * with its statistics. Finally, the [recordingDataFlow] state is updated.
     */
    suspend fun importRecordingFromUri(
        uri: Uri, contentResolver: ContentResolver
    ): Boolean {
        val outputDir = trekMeContext.recordingsDir ?: return false
        val result = geoRecordParser.copyAndParse(uri, contentResolver, outputDir)
        if (result != null) {
            val (geoRecord, file) = result
            addOneRecording(file, geoRecord)
        } else {
            val name = FileUtils.getFileRealFileNameFromURI(contentResolver, uri)
            val fileName = if (name != null && name.isNotEmpty()) name else "file"
            val msg = app.applicationContext.getString(R.string.recording_imported_failure, fileName)
            appEventBus.postMessage(StandardMessage(msg))
        }
        return result != null
    }

    private suspend fun addOneRecording(file: File, geoRecord: GeoRecord) {
        val recordingData = makeRecordingData(file, geoRecord)
        val byFile = recordingDataFlow.value.associateBy { it.file }.toMutableMap()
        byFile[file] = recordingData
        recordingDataFlow.value = byFile.values.toList().mostRecentFirst()
    }

    /**
     * Concurrently parses all recordings and computing statistics.
     */
    private suspend fun initRecordingDataFlow() = withContext(Dispatchers.Default) {
        suspend fun parseAndComputeStats(file: File): GeoRecord? {
            return try {
                geoRecordParser.parse(FileInputStream(file), "A track")
            } catch (e: Exception) {
                Log.e(TAG, "The file ${file.name} was parsed with error ${stackTraceToString(e)}")
                null
            }
        }

        /* Update our data structure with controlled concurrency */
        val coreCount = (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(2)
        val recordingDataList = recordFiles.asFlow().mapNotNull { file ->
            flow {
                val geoRecord = parseAndComputeStats(file)
                emit(geoRecord?.let {
                    makeRecordingData(file, it)
                })
            }
        }.flowOn(ioDispatcher).flattenMerge(coreCount).filterNotNull().toList().mostRecentFirst()

        recordingDataFlow.value = recordingDataList
    }

    private fun List<RecordingData>.mostRecentFirst() = sortedByDescending {
        it.time ?: -1
    }

    private suspend fun makeRecordingData(gpxFile: File, geoRecord: GeoRecord): RecordingData {
        return withContext(ioDispatcher) {
            val routeIds: List<String> = geoRecord.routes.map { it.id }
            val statistics = geoRecord.let {
                TrackTools.getGeoStatistics(it)
            }

            RecordingData(
                gpxFile,
                FileUtils.getFileNameWithoutExtention(gpxFile),
                statistics,
                routeIds,
                geoRecord.time
            )
        }
    }
}

private const val TAG = "GeoRecordRepository"