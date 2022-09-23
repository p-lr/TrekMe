package com.peterlaurence.trekme.core.map.data.dao

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.peterlaurence.trekme.core.map.data.LEGACY_MAP_ROUTE_FILENAME
import com.peterlaurence.trekme.core.map.data.MAP_ROUTES_DIRECTORY
import com.peterlaurence.trekme.core.map.data.MAP_ROUTE_INFO_FILENAME
import com.peterlaurence.trekme.core.map.data.MAP_ROUTE_MARKERS_FILENAME
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.data.models.RouteGson
import com.peterlaurence.trekme.core.map.data.models.RouteInfoKtx
import com.peterlaurence.trekme.core.map.data.models.RouteKtx
import com.peterlaurence.trekme.core.map.domain.dao.RouteDao
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.data.mappers.toDomain
import com.peterlaurence.trekme.core.map.data.mappers.toMarker
import com.peterlaurence.trekme.core.map.data.mappers.toRouteInfoKtx
import com.peterlaurence.trekme.core.map.data.mappers.toRouteKtx
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * When routes are imported in a map, we check whether the map uses the legacy routes format.
 * If the legacy format is used, we convert it on-the-fly.
 * The original routes format was deprecated in September 2021 and should be supported for at least
 * one more year - ideally as long as reasonably possible.
 */
class RouteDaoImpl(
    private val fileBasedMapRegistry: FileBasedMapRegistry,
    private val ioDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher
): RouteDao {
    private val format = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }
    private val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

    /* Associates a route id and the directory name of the serialized route */
    private val routeDirNameForId = mutableMapOf<String, String>()

    override suspend fun importRoutes(map: Map) = withContext(ioDispatcher) {
        routeDirNameForId.clear()
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return@withContext
        val legacyRouteFile = File(directory, LEGACY_MAP_ROUTE_FILENAME)
        val routes = if (legacyRouteFile.exists()) {
            /* Import legacy */
            val routes = getLegacyRoutes(gson, legacyRouteFile)

            /* Convert to new format and serialize */
            routes.forEach { route ->
                saveNewRoute(map, route)
            }

            /* Delete the legacy format */
            legacyRouteFile.safeDelete()

            routes
        } else {
            val dir =
                getOrCreateDirectory(directory, MAP_ROUTES_DIRECTORY) ?: return@withContext
            getRoutes(dir)
        }

        /* Set the routes on the main thread */
        withContext(mainDispatcher) {
            map.setRoutes(routes)
        }
    }

    override suspend fun saveNewRoute(map: Map, route: Route): Unit = withContext(ioDispatcher) {
        runCatching {
            val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return@withContext
            val dir =
                getOrCreateDirectory(directory, MAP_ROUTES_DIRECTORY) ?: return@withContext
            routeDirNameForId[route.id] = route.name ?: route.id
            val routeKtx = route.toRouteKtx()
            val routeInfoKtx = route.toRouteInfoKtx()
            serializeRoute(dir, route.id, routeKtx, routeInfoKtx)
        }
    }

    override suspend fun saveRouteInfo(map: Map, route: Route): Unit = withContext(ioDispatcher) {
        runCatching {
            val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return@withContext
            val dir =
                getOrCreateDirectory(directory, MAP_ROUTES_DIRECTORY) ?: return@withContext
            val routeInfoKtx = route.toRouteInfoKtx()
            serializeRouteInfo(dir, route.id, routeInfoKtx)
        }
    }

    override suspend fun deleteRoute(map: Map, route: Route): Unit = withContext(ioDispatcher) {
        // routes created from 3.0.0 and above have an id serialized
        deleteRoutesUsingId(map, listOf(route.id))

        // legacy routes don't have an id serialized
        deleteRouteUsingDirName(map, route)
    }

    /**
     * Walk through routes on disk. If there's a match between [RouteInfoKtx]'s id and one of the
     * id in the provided [ids], we delete the route.
     * This operation performs quickly since [RouteInfoKtx] is a small object.
     */
    override suspend fun deleteRoutesUsingId(map: Map, ids: List<String>): Unit = withContext(ioDispatcher) {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return@withContext
        val root = File(directory, MAP_ROUTES_DIRECTORY)

        val dirList = runCatching {
            root.listFiles { it: File ->
                it.isDirectory
            }
        }.getOrNull() ?: return@withContext

        dirList.forEach { dir ->
            val infoFile = File(dir, MAP_ROUTE_INFO_FILENAME)
            val routeInfoKtx = runCatching<RouteInfoKtx> {
                FileUtils.getStringFromFile(infoFile).let {
                    format.decodeFromString(it)
                }
            }.getOrNull()

            if (routeInfoKtx?.id in ids) {
                runCatching {
                    dir.deleteRecursively()
                }
            }
        }
    }

    private suspend fun deleteRouteUsingDirName(map: Map, route: Route) = withContext(ioDispatcher) {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return@withContext
        val root = File(directory, MAP_ROUTES_DIRECTORY)
        runCatching {
            val routeDirName = routeDirNameForId[route.id]
            if (routeDirName != null) {
                val mapDir = File(root, routeDirName)
                if (mapDir.exists()) {
                    mapDir.deleteRecursively()
                }
            }
        }
    }

    /**
     * Get the routes. A "routes" folder is expected at the root directory of the map. This "routes"
     * folder contains directories, one per route. Those directories are named after each route id.
     * A route directory contains two files:
     * * A [MAP_ROUTE_INFO_FILENAME] file which contains various information about the route, such
     * as its name, color, etc.
     * * A [MAP_ROUTE_MARKERS_FILENAME] files which contains all markers of the route.
     */
    private fun getRoutes(routesDir: File): List<Route> {
        val dirList = runCatching {
            routesDir.listFiles { it: File ->
                it.isDirectory
            }
        }.getOrNull() ?: return emptyList()

        return dirList.mapNotNull { dir ->
            val infoFile = File(dir, MAP_ROUTE_INFO_FILENAME)
            val routeInfoKtx = runCatching<RouteInfoKtx> {
                FileUtils.getStringFromFile(infoFile).let {
                    format.decodeFromString(it)
                }
            }.getOrNull() ?: return@mapNotNull null

            val markersFile = File(dir, MAP_ROUTE_MARKERS_FILENAME)
            val routeMarkerKtx = runCatching<RouteKtx> {
                FileUtils.getStringFromFile(markersFile).let {
                    format.decodeFromString(it)
                }
            }.getOrNull() ?: return@mapNotNull null

            Route(
                id = routeInfoKtx.id,
                name = routeInfoKtx.name,
                initialColor = routeInfoKtx.color,
                elevationTrusted = routeInfoKtx.elevationTrusted,
                initialVisibility = routeInfoKtx.visible,
                initialMarkers = routeMarkerKtx.markers.map {
                    it.toMarker()
                }.toMutableList()
            ).also {
                routeDirNameForId[it.id] = dir.name
            }
        }
    }


    /**
     * Get the routes, using the legacy format.
     * A file named 'routes.json' (also referred as track file) is expected at the same level of the
     * 'map.json' configuration file. If there is no track file, this means that the map has no routes.
     *
     * This should be called off UI thread.
     *
     * @return A list of [Route]
     */
    private fun getLegacyRoutes(gson: Gson, routeFile: File): List<Route> {
        return try {
            val jsonString = FileUtils.getStringFromFile(routeFile)
            val routeGson = gson.fromJson(jsonString, RouteGson::class.java)
            routeGson.routes.map { it.toDomain() }
        } catch (e: Exception) {
            /* Error while decoding the json file */
            Log.e(TAG, e.message, e)
            emptyList()
        }
    }

    private fun getOrCreateDirectory(parent: File?, dirName: String): File? {
        return runCatching {
            File(parent, dirName).let {
                if (it.exists()) {
                    it
                } else {
                    if (it.mkdir()) it else null
                }
            }
        }.getOrNull()
    }

    private fun serializeRoute(
        routesDir: File,
        id: String,
        routeKtx: RouteKtx,
        routeInfoKtx: RouteInfoKtx
    ) {
        serializeRouteInfo(routesDir, id, routeInfoKtx)
        serializeRouteMarkers(routesDir, id, routeKtx)
    }

    private fun serializeRouteInfo(
        routesDir: File,
        id: String,
        routeInfoKtx: RouteInfoKtx
    ) {
        val routeDirName = routeDirNameForId[id] ?: return
        val dir = getOrCreateDirectory(routesDir, routeDirName) ?: return

        val routeInfoFile = File(dir, MAP_ROUTE_INFO_FILENAME).also {
            if (!it.createNewFile()) {
                Log.e(TAG, "Error while creating $MAP_ROUTE_INFO_FILENAME")
            }
        }
        val routeInfoKtxJson = format.encodeToString(routeInfoKtx)
        FileUtils.writeToFile(routeInfoKtxJson, routeInfoFile)
    }

    private fun serializeRouteMarkers(
        routesDir: File,
        id: String,
        routeKtx: RouteKtx,
    ) {
        val routeDirName = routeDirNameForId[id] ?: return
        val dir = getOrCreateDirectory(routesDir, routeDirName) ?: return

        val routeMarkersFile = File(dir, MAP_ROUTE_MARKERS_FILENAME).also {
            if (!it.createNewFile()) {
                Log.e(TAG, "Error while creating $MAP_ROUTE_MARKERS_FILENAME")
            }
        }
        val routeKtsJson = format.encodeToString(routeKtx)
        FileUtils.writeToFile(routeKtsJson, routeMarkersFile)
    }

    private fun File.safeDelete() = runCatching {
        delete()
    }
}

private const val TAG = "RouteDao"