package com.peterlaurence.trekme.core.map.maploader

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.peterlaurence.trekme.core.map.*
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.*
import com.peterlaurence.trekme.core.map.maploader.events.MapListUpdateEvent
import com.peterlaurence.trekme.core.map.maploader.tasks.MapArchiveSearchTask
import com.peterlaurence.trekme.core.map.maploader.tasks.mapCreationTask
import com.peterlaurence.trekme.core.map.maploader.tasks.mapLandmarkImportTask
import com.peterlaurence.trekme.core.map.maploader.tasks.mapRouteImportTask
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import kotlin.coroutines.resume

/**
 * The MapLoader acts as central point for most operations related to maps.
 * It uses the following tasks defined in [com.peterlaurence.trekme.core.map.maploader.tasks]:
 *
 * * [mapCreationTask] -> create instances of [Map]
 * * [MapDeleteTask] -> delete a [Map]
 * * [MapMarkerImportTask] -> Import the markers of a [Map]
 * * [mapRouteImportTask] -> Import the list of routes for a given [Map]
 * * [MapArchiveSearchTask] -> Get the list of [MapArchive]
 *
 * @author P.Laurence -- converted to Kotlin on 16/02/2019
 */
class MapLoader(
        private val mainDispatcher: CoroutineDispatcher,
        private val defaultDispatcher: CoroutineDispatcher,
        private val ioDispatcher: CoroutineDispatcher
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

    private val _mapListUpdateEventFlow = MutableSharedFlow<MapListUpdateEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val mapListUpdateEventFlow = _mapListUpdateEventFlow.asSharedFlow()

    /**
     * Get a read-only list of [Map]s
     */
    val maps: List<Map>
        get() = mapList

    /**
     * Create once for all the [Gson] object, that is used to serialize/deserialize json content.
     * Register all [Projection] types, depending on their name.
     */
    init {
        val factory = RuntimeTypeAdapterFactory.of(
                Projection::class.java, "projection_name")
        for ((key, value) in projectionHashMap) {
            factory.registerSubtype(value, key)
        }
        gson = GsonBuilder().serializeNulls().setPrettyPrinting().registerTypeAdapterFactory(factory).create()
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

        notifyMapListUpdateListeners()
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
     * Launch a [MapMarkerImportTask] which reads the markers.json file.
     */
    suspend fun getMarkersForMap(map: Map): Boolean = withContext(ioDispatcher) {
        val markerFile = File(map.directory, MAP_MARKER_FILENAME)
        if (!markerFile.exists()) return@withContext false

        val jsonString: String
        return@withContext try {
            jsonString = FileUtils.getStringFromFile(markerFile)
            val markerGson: MarkerGson = gson.fromJson(jsonString, MarkerGson::class.java)
            map.markerGson = markerGson
            true
        } catch (e: Exception) {
            /* Error while decoding the json file */
            Log.e(TAG, e.message, e)
            false
        }
    }

    /**
     * Launch a task which reads the routes.json file.
     * The [mapRouteImportTask] is called off UI thread. Right after, on the calling thread (which
     * should be the UI thread), the result (a nullable instance of [RouteGson]) is set on the [Map]
     * given as parameter.
     */
    suspend fun importRoutesForMap(map: Map) = withContext(ioDispatcher) {
        mapRouteImportTask(map, gson, MAP_ROUTE_FILENAME)
    }?.let { routeGson ->
        map.routeGson = routeGson
    }

    /**
     * Launch a task which reads the landmarks.json file.
     * The [mapLandmarkImportTask] is called off UI thread. Right after, on the calling thread (which
     * should be the UI thread), the result (a nullable instance of [LandmarkGson]) is set on the [Map]
     * given as parameter.
     */
    suspend fun getLandmarksForMap(map: Map) =
            withContext(defaultDispatcher) {
                mapLandmarkImportTask(map, gson, MAP_LANDMARK_FILENAME)
            }?.let { landmarkGson ->
                map.landmarkGson = landmarkGson
            }

    /**
     * Launch a task which gets the list of [MapArchive].
     * It also shows how a java [Thread] can be wrapped inside a coroutine so that it can be used
     * by Kotlin code.
     *
     * TODO: Remove this along with MapArchiveSearchTask class. This logic isn't used anymore.
     */
    @Suppress("unused")
    suspend fun getMapArchiveList(dirs: List<File>): List<MapArchive> = suspendCancellableCoroutine { cont ->
        val task = MapArchiveSearchTask(dirs, MAP_FILENAME, object : MapArchiveListUpdateListener {
            override fun onMapArchiveListUpdate(mapArchiveList: List<MapArchive>) {
                cont.resume(mapArchiveList)
            }
        })

        cont.invokeOnCancellation {
            task.cancel()
        }

        task.start()
    }

    /**
     * Add a [Map] to the internal list and generate the json file. Then, saves the [Map].
     */
    suspend fun addMap(map: Map) {
        /* Add the map */
        withContext(mainDispatcher) {
            map.addIfNew()
        }

        /* Generate the json file */
        saveMap(map)
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
     * Save the content of a [Map], so the changes persist upon application restart.
     * Here, it writes to the corresponding json file.
     * Then, broadcasts an [MapListUpdateEvent].
     *
     * @param map The [Map] to save.
     */
    suspend fun saveMap(map: Map) {
        withContext(ioDispatcher) {
            val jsonString = gson.toJson(map.mapGson)
            val configFile = map.configFile

            writeToFile(jsonString, configFile) {
                Log.e(TAG, "Error while saving the map")
            }
        }

        withContext(mainDispatcher) {
            notifyMapListUpdateListeners()
        }
    }

    /**
     * Saves the [MarkerGson] of a [Map], so the changes persist upon application restart.
     * Here, it writes to the corresponding json file.
     *
     * @param map The [Map] to save.
     */
    suspend fun saveMarkers(map: Map) = withContext(ioDispatcher) {
        val jsonString = gson.toJson(map.markerGson)

        val markerFile = File(map.directory, MAP_MARKER_FILENAME)
        writeToFile(jsonString, markerFile) {
            Log.e(TAG, "Error while saving the markers")
        }
    }

    /**
     * Save the [LandmarkGson] of a [Map], so the changes persist upon application restart.
     * @param map the [Map] to save.
     */
    suspend fun saveLandmarks(map: Map) = withContext(ioDispatcher) {
        val jsonString = gson.toJson(map.landmarkGson)
        val landmarkFile = File(map.directory, MAP_LANDMARK_FILENAME)

        writeToFile(jsonString, landmarkFile) {
            Log.e(TAG, "Error while saving the landmarks")
        }
    }

    /**
     * Save the [RouteGson] of a [Map], so the changes persist upon application restart.
     * Here, it writes to the corresponding json file.
     *
     * @param map The [Map] to save.
     */
    suspend fun saveRoutes(map: Map) = withContext(ioDispatcher) {
        val jsonString = gson.toJson(map.routeGson)
        val routeFile = File(map.directory, MAP_ROUTE_FILENAME)

        writeToFile(jsonString, routeFile) {
            Log.e(TAG, "Error while saving the routes")
        }
    }

    /**
     * Delete a [Map]. Recursively deletes its directory.
     *
     * @param map The [Map] to delete.
     */
    suspend fun deleteMap(map: Map) {
        val mapDirectory = map.directory
        withContext(mainDispatcher) {
            mapList.remove(map)
        }

        /* Notify for view update */
        notifyMapListUpdateListeners()

        /* Delete the map directory in a separate thread */
        withContext(ioDispatcher) {
            FileUtils.deleteRecursive(mapDirectory)
        }
    }

    /**
     * Delete a [MarkerGson.Marker] from a [Map].
     */
    suspend fun deleteMarker(map: Map, marker: MarkerGson.Marker) {
        withContext(mainDispatcher) {
            val markerList = map.markers
            markerList?.remove(marker)
        }

        saveMarkers(map)
    }

    /**
     * Delete a [Landmark] from a [Map].
     */
    suspend fun deleteLandmark(map: Map, landmark: Landmark) {
        withContext(mainDispatcher) {
            map.landmarkGson.landmarks.remove(landmark)
        }

        saveLandmarks(map)
    }

    /**
     * Renaming a map involves two steps:
     * 1. Rename the directory containing files
     * 2. Change the name that appears in map.json
     * After that call, the map.json isn't updated. To update it, invoke [saveMap].
     */
    suspend fun renameMap(map: Map, newName: String) {
        withContext(ioDispatcher) {
            map.directory.renameTo(File(map.directory.parentFile, newName))
            map.name = newName
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

    private fun notifyMapListUpdateListeners() {
        _mapListUpdateEventFlow.tryEmit(MapListUpdateEvent(maps.isNotEmpty()))
    }

    enum class CalibrationMethod {
        SIMPLE_2_POINTS,
        CALIBRATION_3_POINTS,
        CALIBRATION_4_POINTS,
        UNKNOWN;

        companion object {
            fun fromCalibrationName(name: String?): CalibrationMethod {
                if (name != null) {
                    for (method in values()) {
                        if (name.equals(method.toString(), ignoreCase = true)) {
                            return method
                        }
                    }
                }
                return UNKNOWN
            }
        }
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