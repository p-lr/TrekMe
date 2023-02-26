package com.peterlaurence.trekme.core.excursion.data.dao

import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.excursion.data.model.ExcursionFileBased
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class ExcursionDaoFileBased(
    val trekMeContext: TrekMeContext
) : ExcursionDao {
    private val excursions = MutableStateFlow<List<Excursion>>(emptyList())
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    override suspend fun getExcursionsFlow(): StateFlow<List<Excursion>> {
        val excursionFolders = trekMeContext.rootDirList.map {
            File(it, EXCURSIONS_FOLDER_NAME)
        }

        excursions.update {
            excursionSearchTask(CONFIG_FILENAME, *excursionFolders.toTypedArray())
        }
        return excursions
    }

    private fun excursionSearchTask(excursionFileName: String, vararg dirs: File): List<Excursion> {
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

            val waypointsFile = File(rootDir, WAYPOINTS_FILENAME)
            val waypoints = runCatching<List<ExcursionWaypoint>> {
                FileUtils.getStringFromFile(waypointsFile).let {
                    json.decodeFromString(it)
                }
            }.getOrElse { emptyList() }

            excursionList.add(
                ExcursionFileBased(
                    f,
                    config.id,
                    config.title,
                    config.description,
                    config.type.toDomain()
                )
            )
        }

        return excursionList
    }
}

@Serializable
private data class ExcursionConfig(
    val id: String,
    val title: String,
    @SerialName("desc")
    val description: String = "",
    val type: Type,
    @SerialName("photos")
    val photos: List<Photo>
)

@Serializable
enum class Type {
    @SerialName("hike")
    Hike,

    @SerialName("running")
    Running,

    @SerialName("mountain-bike")
    MountainBike,

    @SerialName("travel-bike")
    TravelBike,

    @SerialName("horse-riding")
    HorseRiding,

    @SerialName("aerial")
    Aerial,

    @SerialName("nautical")
    Nautical
}

@Serializable
private data class ExcursionWaypoint(
    val id: String,
    val name: String,
    @SerialName("lat")
    val latitude: Double,
    @SerialName("lon")
    val longitude: Double,
    @SerialName("ele")
    val elevation: Double?,
    val comment: String,
    @SerialName("photos")
    val photos: List<Photo>
)

@Serializable
data class Photo(
    val name: String,
    @SerialName("file")
    val fileName: String,
)

private fun Type.toDomain(): ExcursionType {
    return when (this) {
        Type.Hike -> ExcursionType.Hike
        Type.Running -> ExcursionType.Running
        Type.MountainBike -> ExcursionType.MountainBike
        Type.TravelBike -> ExcursionType.TravelBike
        Type.HorseRiding -> ExcursionType.HorseRiding
        Type.Aerial -> ExcursionType.Aerial
        Type.Nautical -> ExcursionType.Nautical
    }
}


private val TAG = "ExcursionDaoFileBased"

private const val MAX_RECURSION_DEPTH = 5
private const val EXCURSIONS_FOLDER_NAME = "excursions"
private const val CONFIG_FILENAME = "excursion.json"
private const val WAYPOINTS_FILENAME = "waypoints.json"
private const val FILES_DIR = "files"