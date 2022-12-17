package com.peterlaurence.trekme.core.map.data.dao

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.peterlaurence.trekme.core.map.data.MAP_FILENAME
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.data.models.MapGson
import com.peterlaurence.trekme.core.map.data.models.MapPropertiesKtx
import com.peterlaurence.trekme.core.map.domain.dao.MapLoaderDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import com.peterlaurence.trekme.core.map.data.mappers.toDomain
import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class MapLoaderDaoFileBased constructor(
    private val registry: FileBasedMapRegistry,
    private val mapSaverDao: MapSaverDao,
    private val gson: Gson,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher
) : MapLoaderDao {

    /**
     * Parses all [Map]s inside the provided list of directories, and returns a list of [Map].
     *
     * @param dirs The directories in which to search for maps.
     */
    override suspend fun loadMaps(dirs: List<File>): List<Map> {
        if (dirs.isEmpty()) return emptyList()

        return findMaps(dirs)
    }

    /**
     * Launches the search in background thread.
     *
     * @param dirs The directories in which to search for new maps.
     */
    private suspend fun findMaps(dirs: List<File>) = withContext(ioDispatcher) {
        mapCreationTask(gson, MAP_FILENAME, *dirs.toTypedArray())
    }

    /**
     * Search for maps on all provided directories.
     * Parses the json files to, e.g, process calibration information.
     *
     * @author P.Laurence on 30/04/2017 -- converted to Kotlin on 05/05/2019
     */
    private suspend fun mapCreationTask(mGson: Gson, mapFileName: String, vararg dirs: File): List<Map> {
        val mapFilesFoundList = mutableListOf<File>()
        val mapList = mutableListOf<Map>()

        fun recursiveFind(root: File, depth: Int) {
            if (depth > MAX_RECURSION_DEPTH) return

            /* Don't allow nested maps */
            val rootJsonFile = File(root, mapFileName)
            if (rootJsonFile.exists() && rootJsonFile.isFile) {
                mapFilesFoundList.add(rootJsonFile)
                return
            }

            try {
                val list = root.listFiles() ?: return

                for (f in list) {
                    if (f.isDirectory) {
                        recursiveFind(f, depth + 1)
                    }
                }
            } catch (e: Exception) {
                // probably a permission issue, SD-card not mounted, etc.
            }

        }

        /* Search for json files */
        for (dir in dirs) {
            recursiveFind(dir, 1)
        }

        /* Now parse the json files found */
        for (f in mapFilesFoundList) {
            val rootDir = f.parentFile ?: continue

            /* Get json file content as String */
            val jsonString: String
            try {
                jsonString = FileUtils.getStringFromFile(f)
            } catch (e: Exception) {
                // Error while decoding the json file
                Log.e(TAG, e.message, e)
                continue
            }

            try {
                /* json deserialization */
                val mapGson = mGson.fromJson(jsonString, MapGson::class.java)
                val elevationFix = getElevationFix(rootDir)

                /* Map uuid was introduced 2022/09/19, but it should have been done from the start.
                 * This is why we check here if there's an uuid, otherwise we create one and we
                 * save the map (we need the uuid to be persisted for e.g favorites maps to work
                 * properly). */
                val shouldSaveUUID = mapGson.uuid == null

                val thumbnailImage = if (mapGson.thumbnail != null) {
                    getThumbnail(File(rootDir, mapGson.thumbnail))
                } else null

                /* Convert to domain type */
                val mapConfig = mapGson.toDomain(elevationFix, thumbnailImage) ?: continue

                /* Map creation */
                val map = MapFileBased(mapConfig)

                /* Remember map root folder */
                registry.setRootFolder(map.id, rootDir)

                /* See above for explanation */
                if (shouldSaveUUID) {
                    mapSaverDao.save(map)
                }

                mapList.add(map)
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, e.message, e)
            } catch (e: NullPointerException) {
                Log.e(TAG, e.message, e)
            }
        }

        return mapList
    }

    private fun getThumbnail(file: File): Bitmap? {
        val bmOptions = BitmapFactory.Options()
        return BitmapFactory.decodeFile(file.absolutePath, bmOptions)
    }

    private suspend fun getElevationFix(rootDir: File): Int = withContext(ioDispatcher) {
        val propertiesFile = File(rootDir, propertiesFileName)
        if (!propertiesFile.exists()) {
            return@withContext 0
        }
        val str = propertiesFile.readText()
        val properties = json.decodeFromString<MapPropertiesKtx>(str)
        properties.elevationFix
    }
}

private const val MAX_RECURSION_DEPTH = 3
private const val TAG = "MapCreationTask"