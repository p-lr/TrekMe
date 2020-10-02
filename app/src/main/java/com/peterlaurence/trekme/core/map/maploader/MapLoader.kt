package com.peterlaurence.trekme.core.map.maploader

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.MapArchive
import com.peterlaurence.trekme.core.map.gson.*
import com.peterlaurence.trekme.core.map.mapimporter.MapImporter
import com.peterlaurence.trekme.core.map.maploader.events.MapListUpdateEvent
import com.peterlaurence.trekme.core.map.maploader.tasks.*
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import kotlin.coroutines.resume

/**
 * Singleton object that acts as central point for most operations related to the maps.
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
object MapLoader : MapImporter.MapImportListener {
    const val MAP_FILE_NAME = "map.json"
    const val MAP_MARKER_FILE_NAME = "markers.json"
    const val MAP_ROUTE_FILE_NAME = "routes.json"
    const val MAP_LANDMARK_FILE_NAME = "landmarks.json"

    /**
     * All [Projection]s are registered here.
     */
    @JvmStatic
    private val PROJECTION_HASH_MAP = object : HashMap<String, Class<out Projection>>() {
        init {
            put(MercatorProjection.NAME, MercatorProjection::class.java)
            put(UniversalTransverseMercator.NAME, UniversalTransverseMercator::class.java)
        }
    }
    private const val TAG = "MapLoader"

    private val gson: Gson
    private val mapList: MutableList<Map> = mutableListOf()
    private var mapMarkerUpdateListener: MapMarkerUpdateListener? = null

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
        for ((key, value) in PROJECTION_HASH_MAP) {
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
    private suspend fun findMaps(dirs: List<File>) = withContext(Dispatchers.Default) {
        mapCreationTask(gson, *dirs.toTypedArray())
    }

    /**
     * Launch a [MapMarkerImportTask] which reads the markers.json file.
     */
    fun getMarkersForMap(map: Map) {
        val mapMarkerImportTask = MapMarkerImportTask(mapMarkerUpdateListener,
                map, gson)
        mapMarkerImportTask.execute()
    }

    /**
     * Launch a task which reads the routes.json file.
     * The [mapRouteImportTask] is called off UI thread. Right after, on the calling thread (which
     * should be the UI thread), the result (a nullable instance of [RouteGson]) is set on the [Map]
     * given as parameter.
     */
    suspend fun importRoutesForMap(map: Map) = withContext(Dispatchers.Default) {
        mapRouteImportTask(map, gson)
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
            withContext(Dispatchers.Default) {
                mapLandmarkImportTask(map, gson)
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
        val task = MapArchiveSearchTask(dirs, object : MapArchiveListUpdateListener {
            override fun onMapArchiveListUpdate(mapArchiveList: List<MapArchive>) {
                cont.resume(mapArchiveList)
            }
        })

        cont.invokeOnCancellation {
            task.cancel()
        }

        task.start()
    }

    fun setMapMarkerUpdateListener(listener: MapMarkerUpdateListener) {
        mapMarkerUpdateListener = listener
    }

    fun clearMapMarkerUpdateListener() {
        mapMarkerUpdateListener = null
    }

    /**
     * Add a [Map] to the internal list and generate the json file.
     */
    fun addMap(map: Map) {
        /* Add the map */
        map.addIfNew()

        /* Generate the json file */
        saveMap(map)

        /* Notify for view update */
        notifyMapListUpdateListeners()
    }

    /**
     * Add a [Map] to the internal list and generate the json file.
     * This is typically called after an import, after a [Map] has been generated from a file
     * structure.
     */
    override fun onMapImported(map: Map?, status: MapImporter.MapParserStatus) {
        if (map == null) return
        addMap(map)
    }

    override fun onMapImportError(e: MapImporter.MapParseException?) {
        Log.e(TAG, "Error while parsing a map")
        if (e != null) {
            Log.e(TAG, e.message, e)
        }
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
     * Save the content of a [Map], so the changes persist upon application restart. <br></br>
     * Here, it writes to the corresponding json file.
     *
     * @param map The [Map] to save.
     */
    fun saveMap(map: Map) {
        val jsonString = gson.toJson(map.mapGson)
        val configFile = map.configFile

        writeToFile(jsonString, configFile) {
            Log.e(TAG, "Error while saving the map")
        }

        notifyMapListUpdateListeners()
    }

    /**
     * Save the [MarkerGson] of a [Map], so the changes persist upon application restart.
     * Here, it writes to the corresponding json file.
     *
     * @param map The [Map] to save.
     */
    fun saveMarkers(map: Map) {
        val jsonString = gson.toJson(map.markerGson)

        val markerFile = File(map.directory, MAP_MARKER_FILE_NAME)
        writeToFile(jsonString, markerFile) {
            Log.e(TAG, "Error while saving the markers")
        }
    }

    /**
     * Save the [LandmarkGson] of a [Map], so the changes persist upon application restart.
     * @param map the [Map] to save.
     */
    fun saveLandmarks(map: Map) {
        val jsonString = gson.toJson(map.landmarkGson)
        val landmarkFile = File(map.directory, MAP_LANDMARK_FILE_NAME)

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
    fun saveRoutes(map: Map) {
        val jsonString = gson.toJson(map.routeGson)
        val routeFile = File(map.directory, MAP_ROUTE_FILE_NAME)

        writeToFile(jsonString, routeFile) {
            Log.e(TAG, "Error while saving the routes")
        }
    }

    /**
     * Delete a [Map]. Recursively deletes its directory.
     *
     * @param map The [Map] to delete.
     */
    fun deleteMap(map: Map) {
        val mapDirectory = map.directory
        mapList.remove(map)

        /* Notify for view update */
        notifyMapListUpdateListeners()

        /* Delete the map directory in a separate thread */
        val mapDeleteTask = MapDeleteTask(mapDirectory)
        mapDeleteTask.execute()
    }

    /**
     * Delete a [MarkerGson.Marker] from a [Map].
     */
    fun deleteMarker(map: Map, marker: MarkerGson.Marker) {
        val markerList = map.markers
        markerList?.remove(marker)

        saveMarkers(map)
    }

    /**
     * Delete a [Landmark] from a [Map].
     */
    fun deleteLandmark(map: Map, landmark: Landmark) {
        map.landmarkGson.landmarks.remove(landmark)

        saveLandmarks(map)
    }

    /**
     * Mutate the [Projection] of a given [Map].
     *
     * @return true on success, false if something went wrong.
     */
    fun mutateMapProjection(map: Map, projectionName: String): Boolean {
        val projectionType = PROJECTION_HASH_MAP[projectionName] ?: return false
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
        EventBus.getDefault().post(MapListUpdateEvent(maps.isNotEmpty()))
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

    /**
     * When a map's markers are retrieved from their json content, this listener is called.
     */
    interface MapMarkerUpdateListener {
        fun onMapMarkerUpdate()
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
