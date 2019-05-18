package com.peterlaurence.trekme.core.map.maploader

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.MapArchive
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.map.gson.*
import com.peterlaurence.trekme.core.map.mapimporter.MapImporter
import com.peterlaurence.trekme.core.map.maploader.events.MapListUpdateEvent
import com.peterlaurence.trekme.core.map.maploader.tasks.*
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import com.peterlaurence.trekme.model.providers.stream.TileStreamProviderDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
 * @author peterLaurence -- converted to Kotlin on 16/02/2019
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

    private val mGson: Gson
    private val mMapList: MutableList<Map> = mutableListOf()
    private var mMapMarkerUpdateListener: MapMarkerUpdateListener? = null

    /**
     * Get a read-only list of [Map]s
     */
    val maps: List<Map>
        get() = mMapList.toList()

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
        mGson = GsonBuilder().serializeNulls().setPrettyPrinting().registerTypeAdapterFactory(factory).create()
    }

    /**
     * Clears the internal list of [Map] : [mMapList].
     */
    fun clearMaps() {
        mMapList.clear()
    }

    /**
     * TODO: this function was added for compatibility with legacy java code. Remove when possible.
     * Only the signature with a list of [File] should be used.
     */
    fun generateMaps(dir: File): List<Map> {
        return generateMaps(listOf(dir))
    }

    /**
     * TODO: this function was added for compatibility with legacy java code. Remove when possible.
     * Launches the map search then sets the found [Map]s as the internal list of [Map] : [mMapList].
     */
    private fun generateMaps(dirs: List<File>): List<Map> = runBlocking {
        withContext(Dispatchers.Main) {
            val maps = findMaps(dirs)
            mMapList.addAll(maps)
            notifyMapListUpdateListeners()
            maps
        }
    }

    /**
     * Parses all [Map]s inside the provided listof directories, then updates the internal list of
     * [Map] : [mMapList].
     * It is intended to be the only public method of updating the [Map] list.
     *
     * @param dirs The directories in which to search for maps. If not specified, a default value is
     * taken.
     */
    suspend fun getMaps(dirs: List<File> = listOf()): List<Map> {
        val realDirs = if (dirs.isEmpty()) {
            TrekMeContext.mapsDirList
        } else {
            dirs
        }

        val maps = findMaps(realDirs)
        mMapList.addAll(maps)
        return maps
    }

    /**
     * Launches the search in background thread.
     *
     * @param dirs The directories in which to search for new maps.
     */
    private suspend fun findMaps(dirs: List<File>) = withContext(Dispatchers.Default) {
        mapCreationTask(mGson, *dirs.toTypedArray())
    }

    /**
     * Launch a [MapMarkerImportTask] which reads the markers.json file.
     */
    fun getMarkersForMap(map: Map) {
        val mapMarkerImportTask = MapMarkerImportTask(mMapMarkerUpdateListener,
                map, mGson)
        mapMarkerImportTask.execute()
    }

    /**
     * Launch a task which reads the routes.json file.
     * The [mapRouteImportTask] is called off UI thread. Right after, on the calling thread (which
     * should be the UI thread), the result (a nullable instance of [RouteGson]) is set on the [Map]
     * given as parameter.
     */
    suspend fun getRoutesForMap(map: Map) =
            withContext(Dispatchers.Default) {
                mapRouteImportTask(map, mGson)
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
                mapLandmarkImportTask(map, mGson)
            }?.let { landmarkGson ->
                map.landmarkGson = landmarkGson
            }

    /**
     * Launch a task which gets the list of [MapArchive].
     * It also shows how a java [Thread] can be wrapped inside a coroutine so that it can be used
     * by Kotlin code.
     */
    suspend fun getMapArchiveList(): List<MapArchive> = suspendCoroutine { cont ->
        val dirs = listOf(TrekMeContext.defaultAppDir)
        val task = MapArchiveSearchTask(dirs, object : MapArchiveListUpdateListener {
            override fun onMapArchiveListUpdate(mapArchiveList: List<MapArchive>) {
                cont.resume(mapArchiveList)
            }
        })

        task.start()
    }

    fun setMapMarkerUpdateListener(listener: MapMarkerUpdateListener) {
        mMapMarkerUpdateListener = listener
    }

    fun clearMapMarkerUpdateListener() {
        mMapMarkerUpdateListener = null
    }

    /**
     * Add a [Map] to the internal list and generated the json file. <br></br>
     * This is typically called after an import, after a [Map] has been generated from a file
     * structure.
     */
    override fun onMapImported(map: Map?, status: MapImporter.MapParserStatus) {
        if (map == null) return

        /* Set TileStreamProvider */
        applyTileStreamProviderTo(map)

        /* Add the map */
        mMapList.add(map)

        /* Generate the json file */
        saveMap(map)

        /* Notify for view update */
        notifyMapListUpdateListeners()
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
        return mMapList.firstOrNull { it.id == mapId }
    }

    /**
     * Save the content of a [Map], so the changes persist upon application restart. <br></br>
     * Here, it writes to the corresponding json file.
     *
     *
     * Then, call all registered [MapListUpdateListener].
     *
     * @param map The [Map] to save.
     */
    fun saveMap(map: Map) {
        val jsonString = mGson.toJson(map.mapGson)
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
        val jsonString = mGson.toJson(map.markerGson)

        val markerFile = File(map.directory, MapLoader.MAP_MARKER_FILE_NAME)
        writeToFile(jsonString, markerFile) {
            Log.e(TAG, "Error while saving the markers")
        }
    }

    /**
     * Save the [LandmarkGson] of a [Map], so the changes persist upon application restart.
     * @param map the [Map] to save.
     */
    fun saveLandmarks(map: Map) {
        val jsonString = mGson.toJson(map.landmarkGson)
        val landmarkFile = File(map.directory, MapLoader.MAP_LANDMARK_FILE_NAME)

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
        val jsonString = mGson.toJson(map.routeGson)
        val routeFile = File(map.directory, MapLoader.MAP_ROUTE_FILE_NAME)

        writeToFile(jsonString, routeFile) {
            Log.e(TAG, "Error while saving the routes")
        }
    }

    /**
     * Delete a [Map]. Recursively deletes its directory.
     *
     * @param map The [Map] to delete.
     */
    fun deleteMap(map: Map, listener: MapDeletedListener?) {
        val mapDirectory = map.directory
        mMapList.remove(map)

        /* Notify for view update */
        notifyMapListUpdateListeners()

        /* Delete the map directory in a separate thread */
        val mapDeleteTask = MapDeleteTask(mapDirectory)
        mapDeleteTask.execute()

        listener?.onMapDeleted()
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
        val projectionType = PROJECTION_HASH_MAP[projectionName]
        try {
            val projection = projectionType!!.newInstance()
            map.projection = projection
        } catch (e: InstantiationException) {
            // wrong projection name
            return false
        } catch (e: IllegalAccessException) {
            return false
        }

        return true
    }

    private fun notifyMapListUpdateListeners() {
        EventBus.getDefault().post(MapListUpdateEvent(maps.isNotEmpty()))
    }

    enum class CALIBRATION_METHOD {
        SIMPLE_2_POINTS,
        CALIBRATION_3_POINTS,
        CALIBRATION_4_POINTS,
        UNKNOWN;

        companion object {

            fun fromCalibrationName(name: String?): CALIBRATION_METHOD {
                if (name != null) {
                    for (method in CALIBRATION_METHOD.values()) {
                        if (name.equals(method.toString(), ignoreCase = true)) {
                            return method
                        }
                    }
                }
                return UNKNOWN
            }
        }
    }

    interface MapListUpdateListener {
        fun onMapListUpdate(mapsFound: Boolean)
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

    interface MapDeletedListener {
        fun onMapDeleted()
    }

    /**
     * Assign a [TileStreamProvider] to a [Map], if the origin of the [Map] is known.
     */
    fun applyTileStreamProviderTo(map: Map) {
        when (map.origin) {
            Map.MapOrigin.VIPS -> map.tileStreamProvider = TileStreamProviderDefault(map.directory, map.imageExtension)
            Map.MapOrigin.UNDEFINED -> {
                Log.e(TAG, "Unknown map origin ${map.origin}")
            }
        }
    }

    /**
     * Utility method to write a [String] into a [File].
     */
    private fun writeToFile(st: String, out: File, errCb: () -> Unit) {
        val writer = PrintWriter(out)
        try {
            writer.print(st)
        } catch (e: IOException) {
            errCb()
            Log.e(TAG, e.message, e)
        } finally {
            writer.close()
        }
    }
}
