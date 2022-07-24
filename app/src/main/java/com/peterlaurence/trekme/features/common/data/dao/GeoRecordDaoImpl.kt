package com.peterlaurence.trekme.features.common.data.dao

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.supportedGeoRecordFilesExtensions
import com.peterlaurence.trekme.core.track.TrackTools
import com.peterlaurence.trekme.data.fileprovider.TrekmeFilesProvider
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.common.domain.dao.GeoRecordDao
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.stackTraceToString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileInputStream
import java.util.*

class GeoRecordDaoImpl(
    private val trekMeContext: TrekMeContext,
    private val app: Application,
    private val geoRecordParser: GeoRecordParser,
    private val ioDispatcher: CoroutineDispatcher,
    private val appEventBus: AppEventBus,
    private val gpxRecordEvents: GpxRecordEvents
): GeoRecordDao {
    private val primaryScope = ProcessLifecycleOwner.get().lifecycleScope
    private val contentResolver = app.applicationContext.contentResolver
    private val supportedFileFilter = filter@{ dir: File, filename: String ->
        /* We only look at files */
        if (File(dir, filename).isDirectory) {
            return@filter false
        }

        supportedGeoRecordFilesExtensions.any { filename.endsWith(".$it") }
    }

    private val geoRecordFlow = MutableStateFlow<List<GeoRecord>>(emptyList())

    init {
        primaryScope.launch {
            gpxRecordEvents.gpxFileWriteEvent.collect {
                /* Remember the file <-> id association */
                fileForId[it.geoRecord.id] = it.gpxFile

                geoRecordFlow.value = geoRecordFlow.value + it.geoRecord
            }
        }

        primaryScope.launch {
            initGeoRecords()
        }
    }

    /**
     * Get the list of [File] which extension is in the list of supported extension for track
     * file. Files are searched into the [TrekMeContext.recordingsDir].
     */
    private val recordFiles: Array<File>
        get() = trekMeContext.recordingsDir?.listFiles(supportedFileFilter) ?: emptyArray()

    private val fileForId = mutableMapOf<UUID, File>()

    override fun getGeoRecordsFlow(): StateFlow<List<GeoRecord>> {
        return geoRecordFlow
    }

    override fun getUri(id: UUID): Uri? {
        return fileForId[id]?.let {
            TrekmeFilesProvider.generateUri(it)
        }
    }

    override suspend fun getRecord(id: UUID): GeoRecord? {
        return fileForId[id]?.let {
            geoRecordParser.parse(FileInputStream(it), it.name)
        }
    }

    /**
     * Resolves the given uri to an actual file, and copies it to the app's storage location for
     * recordings. Then, the copied file is parsed to get the corresponding [GeoRecord] instance.
     */
    override suspend fun importRecordingFromUri(
        uri: Uri
    ): GeoRecord? {
        val outputDir = trekMeContext.recordingsDir ?: return null
        val result = geoRecordParser.copyAndParse(uri, contentResolver, outputDir)
        return if (result != null) {
            val (geoRecord, file) = result
            fileForId[geoRecord.id] = file
            geoRecordFlow.value = geoRecordFlow.value + geoRecord
            geoRecord
        } else {
            // TODO: this isn't the responsibility of the data layer
            val name = FileUtils.getFileRealFileNameFromURI(contentResolver, uri)
            val fileName = if (name != null && name.isNotEmpty()) name else "file"
            val msg = app.applicationContext.getString(R.string.recording_imported_failure, fileName)
            appEventBus.postMessage(StandardMessage(msg))
            null
        }
    }

    override suspend fun renameRecording(id: UUID, newName: String): Boolean {
        val existing = geoRecordFlow.value.firstOrNull { it.id == id }
        if (existing != null) {
            geoRecordFlow.value = geoRecordFlow.value - existing + existing.copy(name = newName)
        }

        val file = fileForId[id] ?: return false
        val newFile = File(file.parent, newName + "." + FileUtils.getFileExtension(file))
        fileForId[id] = newFile

        return withContext(Dispatchers.IO) {
            TrackTools.renameGpxFile(file, newFile)
        }
    }

    override suspend fun deleteRecordings(ids: List<UUID>): Boolean = coroutineScope {
        var success = true
        for (id in ids) {
            val file = fileForId[id] ?: continue
            launch(Dispatchers.IO) {
                runCatching {
                    if (file.exists()) {
                        if (!file.delete()) {
                            success = false
                        } else {
                            fileForId.remove(id)
                            val existing = geoRecordFlow.value.firstOrNull { it.id == id }
                            if (existing != null) {
                                geoRecordFlow.value = geoRecordFlow.value - existing
                            }
                        }
                    }
                }.onFailure {
                    success = false
                }
            }
        }

        success
    }

    private suspend fun parse(file: File): GeoRecord? {
        return try {
            geoRecordParser.parse(FileInputStream(file), file.nameWithoutExtension)
        } catch (e: Exception) {
            Log.e("GeoRecord", "The file ${file.name} was parsed with error ${stackTraceToString(e)}")
            null
        }
    }

    private suspend fun initGeoRecords() {
        val concurrency = (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(2)
        val geoRecordList = recordFiles.asFlow().mapNotNull { file ->
            flow {
                val geoRecord = parse(file)
                if (geoRecord != null) {
                    /* Remember the file <-> id association */
                    fileForId[geoRecord.id] = file

                    emit(geoRecord)
                }
            }
        }.flowOn(ioDispatcher).flattenMerge(concurrency).toList()

        geoRecordFlow.value = geoRecordList
    }
}