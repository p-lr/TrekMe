package com.peterlaurence.trekme.core.excursion.data.dao

import android.net.Uri
import androidx.core.net.toUri
import com.peterlaurence.trekme.core.excursion.data.model.ExcursionConfig
import com.peterlaurence.trekme.core.excursion.data.model.ExcursionFileBased
import com.peterlaurence.trekme.core.excursion.data.model.Waypoint
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class ExcursionDaoFileBased(
    private val excursionFolders: List<File>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExcursionDao {
    private val excursions = MutableStateFlow<List<Excursion>>(emptyList())
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    override suspend fun getExcursionsFlow(): StateFlow<List<Excursion>> {
        excursions.update {
            excursionSearchTask(CONFIG_FILENAME, *excursionFolders.toTypedArray())
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

        for (dir in dirs) {
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

    override suspend fun getGeoRecordUri(excursion: Excursion): Uri? {
        val root = (excursion as? ExcursionFileBased)?.root ?: return null

        return root.listFiles()?.firstOrNull {
            it.isFile && it.name.endsWith(".gpx")
        }?.toUri()
    }
}


private val TAG = "ExcursionDaoFileBased"

private const val MAX_RECURSION_DEPTH = 3
private const val CONFIG_FILENAME = "excursion.json"
private const val WAYPOINTS_FILENAME = "waypoints.json"
private const val FILES_DIR = "files"