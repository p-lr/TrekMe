package com.peterlaurence.trekme.core.excursion.data.dao

import com.peterlaurence.trekme.core.excursion.data.mapper.toData
import com.peterlaurence.trekme.core.excursion.data.model.ExcursionConfig
import com.peterlaurence.trekme.core.excursion.data.model.ExcursionFileBased
import com.peterlaurence.trekme.core.excursion.data.model.Waypoint
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.georecord.data.mapper.toGpx
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.lib.gpx.writeGpx
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExcursionDaoFileBased(
    private val rootFolders: List<File>,
    private val appDirFlow: Flow<File>,
    private val geoRecordParser: suspend (file: File) -> GeoRecord?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExcursionDao {
    private val excursions = MutableStateFlow<List<Excursion>>(emptyList())
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    override suspend fun getExcursionsFlow(): StateFlow<List<Excursion>> {
        excursions.update {
            excursionSearchTask(CONFIG_FILENAME, *rootFolders.toTypedArray())
        }
        return excursions
    }

    private suspend fun excursionSearchTask(
        excursionFileName: String, vararg dirs: File
    ): List<Excursion> = withContext(ioDispatcher) {
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

        val excursionList = mutableListOf<Excursion>()
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

    override suspend fun getGeoRecord(excursion: Excursion): GeoRecord? {
        val root = (excursion as? ExcursionFileBased)?.root ?: return null

        val file = root.listFiles()?.firstOrNull {
            it.isFile && it.name.endsWith(".gpx")
        } ?: return null

        return geoRecordParser(file)
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

            val date = Date()
            val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.ENGLISH)
            val folderName = "excursion-" + dateFormat.format(date)

            val destFolder = File(excursionsFolder, folderName)
            if (!destFolder.exists()) {
                destFolder.mkdirs()
            }

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
        }.isSuccess
    }
}


private val TAG = "ExcursionDaoFileBased"

private const val MAX_RECURSION_DEPTH = 3
private const val CONFIG_FILENAME = "excursion.json"
private const val WAYPOINTS_FILENAME = "waypoints.json"
private const val GPX_FILENAME = "track.gpx"
private const val DIR_NAME = "excursions"