package com.peterlaurence.trekme.core.map.maploader

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.peterlaurence.trekme.core.map.*
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.data.*
import com.peterlaurence.trekme.core.map.data.models.LandmarkGson
import com.peterlaurence.trekme.core.map.data.models.MarkerGson
import com.peterlaurence.trekme.core.map.data.models.RuntimeTypeAdapterFactory
import com.peterlaurence.trekme.core.map.domain.interactors.SaveMapInteractor
import com.peterlaurence.trekme.core.map.maploader.tasks.mapCreationTask
import com.peterlaurence.trekme.core.map.mappers.toEntity
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import com.peterlaurence.trekme.core.repositories.map.MapListUpdateRepository
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.*

/**
 * The MapLoader acts as central point for most operations related to maps.
 * It uses the following tasks defined in [com.peterlaurence.trekme.core.map.maploader.tasks]:
 *
 * * Create instances of [Map]
 * * Deletion a [Map]
 *
 * @author P.Laurence -- converted to Kotlin on 16/02/2019
 */
class MapLoader(
    private val mainDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val mapListUpdateRepository: MapListUpdateRepository,
    private val saveMapInteractor: SaveMapInteractor
) {
    /**
     * All [Projection]s are registered here.
     */
    private val projectionHashMap = object : HashMap<String, Class<out Projection>>() {
        init {
            put(MercatorProjection.NAME, MercatorProjection::class.java)
            put(UniversalTransverseMercator.NAME, UniversalTransverseMercator::class.java)
        }
    }

    private val gson: Gson
    private val mapList: MutableList<Map> = mutableListOf()

    /**
     * Get a read-only list of [Map]s
     */
    val maps: List<Map>
        get() = mapList

    /**
     * Create once for all the Gson object, that is used to serialize/deserialize json content.
     * Register all projection types, depending on their name.
     */
    init {
        val factory = RuntimeTypeAdapterFactory.of(
            Projection::class.java, "projection_name"
        )
        for ((key, value) in projectionHashMap) {
            factory.registerSubtype(value, key)
        }
        gson =
            GsonBuilder().serializeNulls().setPrettyPrinting().registerTypeAdapterFactory(factory)
                .create()
    }

    /**
     * Clears the internal list of [Map] : [mapList].
     */
    fun clearMaps() {
        mapList.clear()
    }

    private fun Map.addIfNew() {
        if (this !in mapList) {
            mapList.add(this)
        }
    }

    /**
     * Parses all [Map]s inside the provided list of directories, then updates the internal list of
     * [Map] : [mapList].
     * It is intended to be the only public method of updating the [Map] list.
     *
     * @param dirs The directories in which to search for maps.
     */
    suspend fun updateMaps(dirs: List<File>): List<Map> {
        if (dirs.isEmpty()) return emptyList()

        val maps = findMaps(dirs)

        /* Add the map only if it's indeed a new one */
        maps.forEach {
            it.addIfNew()
        }

        mapListUpdateRepository.notifyMapListUpdate()
        return maps
    }

    /**
     * Launches the search in background thread.
     *
     * @param dirs The directories in which to search for new maps.
     */
    private suspend fun findMaps(dirs: List<File>) = withContext(defaultDispatcher) {
        mapCreationTask(gson, MAP_FILENAME, *dirs.toTypedArray())
    }

    /**
     * Add a [Map] to the internal list and generate the json file. Then, saves the [Map].
     */
    suspend fun addMap(map: Map) {
        /* Add the map */
        withContext(mainDispatcher) {
            map.addIfNew()
        }

        saveMapInteractor.saveMap(map)
    }

    /**
     * Get a [Map] from its id.
     *
     * @return the [Map] or `null` if the given id is unknown.
     */
    fun getMap(mapId: Int): Map? {
        return mapList.firstOrNull { it.id == mapId }
    }

    /**
     * Saves the [MarkerGson] of a [Map], so the changes persist upon application restart.
     * Here, it writes to the corresponding json file.
     *
     * @param map The [Map] to save.
     */
    suspend fun saveMarkers(map: Map) = withContext(mainDispatcher) {
        val markerGson =
            MarkerGson().apply { markers = map.markers?.map { it.toEntity() } ?: listOf() }
        val jsonString = gson.toJson(markerGson)

        withContext(ioDispatcher) {
            val markerFile = File(map.directory, MAP_MARKER_FILENAME)
            writeToFile(jsonString, markerFile) {
                Log.e(TAG, "Error while saving the markers")
            }
        }
    }

    /**
     * Save the [LandmarkGson] of a [Map], so the changes persist upon application restart.
     * @param map the [Map] to save.
     */
    suspend fun saveLandmarks(map: Map) = withContext(mainDispatcher) {
        val jsonString = gson.toJson(LandmarkGson(map.landmarks ?: listOf()))

        withContext(ioDispatcher) {
            val landmarkFile = File(map.directory, MAP_LANDMARK_FILENAME)
            writeToFile(jsonString, landmarkFile) {
                Log.e(TAG, "Error while saving the landmarks")
            }
        }
    }

    /**
     * Delete a [Map]. Recursively deletes its directory.
     *
     * @param map The [Map] to delete.
     */
    suspend fun deleteMap(map: Map) = withContext(mainDispatcher) {
        mapList.remove(map)

        /* Notify for view update */
        mapListUpdateRepository.notifyMapListUpdate()

        /* Delete the map directory in a separate thread */
        val mapDirectory = map.directory
        withContext(ioDispatcher) {
            FileUtils.deleteRecursive(mapDirectory)
        }
    }

    /**
     * Renaming a map involves two steps:
     * 1. Immediately change the name in memory, in the main thread,
     * 2. Rename the directory containing files, using [ioDispatcher],
     * 3. Update the map's directory, if the rename succeeded.
     * After that call, the map.json isn't updated. To update it, invoke [saveMap].
     */
    suspend fun renameMap(map: Map, newName: String) = withContext(mainDispatcher) {
        map.name = newName
        val directory = map.directory ?: return@withContext
        val newDirectory = File(directory.parentFile, newName)
        val renameOk = withContext(ioDispatcher) {
            runCatching {
                directory.renameTo(newDirectory)
            }.getOrNull() ?: false
        }
        if (renameOk) {
            map.directory = newDirectory
        }
    }

    /**
     * Mutate the [Projection] of a given [Map].
     *
     * @return true on success, false if something went wrong.
     */
    fun mutateMapProjection(map: Map, projectionName: String): Boolean {
        val projectionType = projectionHashMap[projectionName] ?: return false
        try {
            val projection = projectionType.newInstance()
            map.projection = projection
        } catch (e: InstantiationException) {
            // wrong projection name
            return false
        } catch (e: IllegalAccessException) {
            return false
        } catch (e: ExceptionInInitializerError) {
            return false
        }

        return true
    }

    interface MapArchiveListUpdateListener {
        fun onMapArchiveListUpdate(mapArchiveList: List<MapArchive>)
    }

    /**
     * Utility method to write a [String] into a [File].
     */
    private fun writeToFile(st: String, out: File, errCb: () -> Unit) {
        try {
            PrintWriter(out).use {
                it.print(st)
            }
        } catch (e: IOException) {
            errCb()
            Log.e(TAG, e.message, e)
        }
    }
}

private const val TAG = "MapLoader"