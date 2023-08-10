package com.peterlaurence.trekme.core.georecord.data.dao

import android.app.Application
import android.net.Uri
import android.util.Log
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.georecord.data.mapper.toGpx
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordDao
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecordLightWeight
import com.peterlaurence.trekme.core.georecord.domain.model.supportedGeoRecordFilesExtensions
import com.peterlaurence.trekme.core.lib.gpx.writeGpx
import com.peterlaurence.trekme.core.georecord.app.TrekmeFilesProvider
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.stackTraceToString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GeoRecordDaoFileBased(
    private val trekMeContext: TrekMeContext,
    private val app: Application,
    private val geoRecordParser: GeoRecordParser,
    private val appEventBus: AppEventBus,
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher
): GeoRecordDao {
    private val contentResolver = app.applicationContext.contentResolver
    private val supportedFileFilter = filter@{ dir: File, filename: String ->
        /* We only look at files */
        if (File(dir, filename).isDirectory) {
            return@filter false
        }

        supportedGeoRecordFilesExtensions.any { filename.endsWith(".$it") }
    }
    private val fileForId = ConcurrentHashMap<UUID, File>()

    private val geoRecordFlow = MutableStateFlow<List<GeoRecordLightWeight>>(emptyList())

    init {
        initGeoRecords()
    }

    /**
     * Get the list of [File] which extension is in the list of supported extension for track
     * file. Files are searched into the [TrekMeContext.recordingsDir].
     */
    private val recordFiles: Array<File>
        get() = trekMeContext.recordingsDir?.listFiles(supportedFileFilter) ?: emptyArray()


    override fun getGeoRecordsFlow(): StateFlow<List<GeoRecordLightWeight>> {
        return geoRecordFlow
    }

    override fun getUri(id: UUID): Uri? {
        return fileForId[id]?.let {
            TrekmeFilesProvider.generateUri(it)
        }
    }

    override suspend fun getRecord(id: UUID): GeoRecord? {
        return fileForId[id]?.let { file ->
            parse(file)?.copy(id = id)
        }
    }

    /**
     * Resolves the given uri to an actual file, and copies it to the app's storage location for
     * recordings. Then, the copied file is parsed to get the corresponding [GeoRecord] instance.
     */
    override suspend fun importGeoRecordFromUri(
        uri: Uri
    ): GeoRecord? {
        val outputDir = trekMeContext.recordingsDir ?: return null
        val result = geoRecordParser.copyAndParse(uri, contentResolver, outputDir)
        return if (result != null) {
            val (geoRecord, file) = result
            fileForId[geoRecord.id] = file
            geoRecordFlow.value = geoRecordFlow.value + GeoRecordLightWeight(geoRecord.id, geoRecord.name)
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

    override suspend fun renameGeoRecord(id: UUID, newName: String): Boolean {
        val existing = geoRecordFlow.value.firstOrNull { it.id == id }
        if (existing != null) {
            geoRecordFlow.value = geoRecordFlow.value - existing + GeoRecordLightWeight(id, newName)
        }

        val file = fileForId[id] ?: return false
        val newFile = File(file.parent, newName + "." + FileUtils.getFileExtension(file))
        fileForId[id] = newFile

        return withContext(Dispatchers.IO) {
            renameGpxFile(file, newFile)
        }
    }

    override suspend fun updateGeoRecord(geoRecord: GeoRecord): Boolean {
        val file = fileForId[geoRecord.id] ?: return false

        return runCatching {
            val gpx = geoRecord.toGpx()
            withContext(ioDispatcher) {
                writeGpx(gpx, FileOutputStream(file))
            }
        }.isSuccess
    }

    override suspend fun deleteGeoRecords(ids: List<UUID>): Boolean = coroutineScope {
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
                            // Atomically update the list
                            geoRecordFlow.update { current ->
                                val existing = current.firstOrNull { it.id == id }
                                if (existing != null) {
                                    current - existing
                                } else current
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

    private fun initGeoRecords() {
        val geoRecords = recordFiles.map { file ->
            val id = UUID.randomUUID()
            fileForId[id] = file
            GeoRecordLightWeight(id, file.nameWithoutExtension)
        }

        geoRecordFlow.value = geoRecords
    }

    private fun renameGpxFile(gpxFile: File, newFile: File): Boolean {
        return runCatching {
            gpxFile.renameTo(newFile)
        }.getOrElse { false }
    }
}