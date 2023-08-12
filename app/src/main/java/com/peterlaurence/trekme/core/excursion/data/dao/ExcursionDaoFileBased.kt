package com.peterlaurence.trekme.core.excursion.data.dao

import android.net.Uri
import com.peterlaurence.trekme.core.excursion.data.mapper.toData
import com.peterlaurence.trekme.core.excursion.data.model.ExcursionConfig
import com.peterlaurence.trekme.core.excursion.data.model.ExcursionFileBased
import com.peterlaurence.trekme.core.excursion.data.model.Type
import com.peterlaurence.trekme.core.excursion.data.model.Waypoint
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.georecord.app.TrekmeFilesProvider
import com.peterlaurence.trekme.core.georecord.data.mapper.gpxToDomain
import com.peterlaurence.trekme.core.georecord.data.mapper.toGpx
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.lib.gpx.parseGpxSafely
import com.peterlaurence.trekme.core.lib.gpx.writeGpx
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExcursionDaoFileBased(
    private val rootFolders: StateFlow<List<File>>,
    private val appDirFlow: Flow<File>,
    private val uriReader: suspend (uri: Uri, reader: suspend (FileInputStream) -> Unit) -> Unit,
    private val nameReaderUri: (Uri) -> String?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExcursionDao {
    private val excursions = MutableStateFlow<List<ExcursionFileBased>>(emptyList())
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    override suspend fun getExcursionsFlow(): StateFlow<List<Excursion>> {
        val folders = rootFolders.first { it.isNotEmpty() }
        excursions.update {
            excursionSearchTask(CONFIG_FILENAME, *folders.toTypedArray())
        }
        return excursions
    }

    private suspend fun excursionSearchTask(
        excursionFileName: String, vararg dirs: File
    ): List<ExcursionFileBased> = withContext(ioDispatcher) {
        val excursionFilesFoundList = mutableListOf<File>()

        @Throws(SecurityException::class)
        fun recursiveFind(root: File, depth: Int) {
            if (depth > MAX_RECURSION_DEPTH) return

            val rootJsonFile = File(root, excursionFileName)
            if (rootJsonFile.exists() && rootJsonFile.isFile) {
                excursionFilesFoundList.add(rootJsonFile)
                /* Don't allow nested excursions */
                return
            }
            val list = root.listFiles() ?: return
            for (f in list) {
                if (f.isDirectory) {
                    recursiveFind(f, depth + 1)
                }
            }
        }

        val excursionFolders = dirs.map { File(it, DIR_NAME) }
        for (dir in excursionFolders) {
            recursiveFind(dir, 1)
        }

        val excursionList = mutableListOf<ExcursionFileBased>()
        for (f in excursionFilesFoundList) {
            val rootDir = f.parentFile ?: continue

            val config = runCatching<ExcursionConfig> {
                FileUtils.getStringFromFile(f).let {
                    json.decodeFromString(it)
                }
            }.getOrNull() ?: continue

            excursionList.add(ExcursionFileBased(rootDir, config))
        }

        excursionList
    }

    override suspend fun getWaypoints(excursion: Excursion): List<ExcursionWaypoint> {
        val root = (excursion as? ExcursionFileBased)?.root ?: return emptyList()

        val waypointsFile = File(root, WAYPOINTS_FILENAME)
        val waypoints = runCatching<List<Waypoint>> {
            FileUtils.getStringFromFile(waypointsFile).let {
                json.decodeFromString(it)
            }
        }.getOrElse { emptyList() }
        return waypoints
    }

    /**
     * Beware that this implementation does not provide a stable id for the returned [GeoRecord].
     * Everytime [getGeoRecord] is called, a new [GeoRecord] with a different id is generated.
     */
    override suspend fun getGeoRecord(excursion: Excursion): GeoRecord? {
        val root = (excursion as? ExcursionFileBased)?.root ?: return null

        val file = root.getGpxFile() ?: return null

        return parseGpxFile(file)
    }

    override fun getGeoRecordUri(id: String): Uri? {
        val excursion = excursions.value.firstOrNull {
            it.id == id
        } ?: return null

        val file = excursion.root.getGpxFile() ?: return null
        return TrekmeFilesProvider.generateUri(file)
    }

    override suspend fun putExcursion(id: String, uri: Uri): Boolean {
        return runCatching {
            val root = appDirFlow.firstOrNull() ?: return false
            val excursionsFolder = File(root, DIR_NAME)

            withContext(ioDispatcher) {
                uriReader(uri) { origStream ->
                    val name = nameReaderUri(uri) ?: GPX_FILENAME

                    /* We create dirs named after the gpx file name, and not using the current
                     * date, since multiple uris can be imported concurrently hence they would end-up
                     * with the same dir name. */
                    val destFolder = File(excursionsFolder, name.substringBeforeLast("."))
                    destFolder.mkdir()

                    val destFile = File(destFolder, name)
                    FileOutputStream(destFile).use { out ->
                        origStream.copyTo(out)
                    }
                    val geoRecord = parseGpxFile(destFile)!! // on purpose

                    /* Config File */
                    val configFile = File(destFolder, CONFIG_FILENAME).also {
                        it.createNewFile()
                    }

                    val config = ExcursionConfig(
                        id,
                        title = geoRecord.name,
                        description = "",
                        type = Type.Hike,
                        photos = emptyList()
                    )
                    val str = json.encodeToString(config)
                    FileUtils.writeToFile(str, configFile)

                    /* Update the state */
                    excursions.update {
                        it + ExcursionFileBased(destFolder, config)
                    }
                }
            }
        }.isSuccess
    }

    override suspend fun putExcursion(
        id: String,
        title: String,
        type: ExcursionType,
        description: String,
        geoRecord: GeoRecord
    ): Boolean {
        return runCatching {
            val root = appDirFlow.firstOrNull() ?: return false
            val excursionsFolder = File(root, DIR_NAME)

            val destFolder = newExcursionFolder(excursionsFolder)

            /* Config File */
            val configFile = File(destFolder, CONFIG_FILENAME).also {
                it.createNewFile()
            }

            val config = ExcursionConfig(id, title, description, type.toData(), photos = emptyList())
            val str = json.encodeToString(config)
            FileUtils.writeToFile(str, configFile)

            /* Gpx file */
            val gpxFile = File(destFolder, GPX_FILENAME).also {
                it.createNewFile()
            }

            val gpx = geoRecord.toGpx()
            withContext(ioDispatcher) {
                writeGpx(gpx, FileOutputStream(gpxFile))
            }

            /* Update the state */
            excursions.update {
                it + ExcursionFileBased(destFolder, config)
            }
        }.isSuccess
    }

    override suspend fun deleteExcursions(ids: List<String>): Boolean {
        val excursionsToDelete = excursions.value.filter {
            it.id in ids
        }

        return withContext(ioDispatcher) {
            excursionsToDelete.all { exc ->
                runCatching {
                    exc.root.deleteRecursively().also { success ->
                        if (success) {
                            excursions.update {
                                it - exc
                            }
                        }
                    }
                }.getOrElse { false }
            }
        }
    }

    override suspend fun rename(id: String, newName: String): Boolean {
        val excursionToRename = excursions.value.firstOrNull {
            it.id == id
        } ?: return false

        return runCatching {
            withContext(ioDispatcher) {
                val newConfig = excursionToRename.config.copy(title = newName)
                val configFile = File(excursionToRename.root, CONFIG_FILENAME)
                val str = json.encodeToString(newConfig)
                FileUtils.writeToFile(str, configFile)

                excursions.update {
                    it - excursionToRename + ExcursionFileBased(excursionToRename.root, newConfig)
                }
            }
        }.isSuccess
    }

    override suspend fun updateGeoRecord(id: String, geoRecord: GeoRecord): Boolean {
        val excursion = excursions.value.firstOrNull {
            it.id == id
        } ?: return false
        val root = (excursion as? ExcursionFileBased)?.root ?: return false

        val file = root.getGpxFile() ?: return false
        return runCatching {
            val gpx = geoRecord.toGpx()
            withContext(ioDispatcher) {
                writeGpx(gpx, FileOutputStream(file))
            }
        }.isSuccess
    }

    private fun File.getGpxFile(): File? = runCatching {
        listFiles()?.firstOrNull {
            it.isFile && it.name.endsWith(".gpx")
        }
    }.getOrNull()

    private fun newExcursionFolder(parent: File): File {
        val date = Date()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.ENGLISH)
        val folderName = "excursion-" + dateFormat.format(date)

        val destFolder = File(parent, folderName)
        if (!destFolder.exists()) {
            destFolder.mkdirs()
        }
        return destFolder
    }

    private suspend fun parseGpxFile(file: File, name: String = file.nameWithoutExtension): GeoRecord? = withContext(ioDispatcher) {
        FileInputStream(file).use {
            parseGpxSafely(it)?.let { gpx ->
                gpxToDomain(gpx, name)
            }
        }
    }
}


private val TAG = "ExcursionDaoFileBased"

private const val MAX_RECURSION_DEPTH = 3
private const val CONFIG_FILENAME = "excursion.json"
private const val WAYPOINTS_FILENAME = "waypoints.json"
private const val GPX_FILENAME = "track.gpx"
private const val DIR_NAME = "excursions"