package com.peterlaurence.trekme.core.map.data.dao

import android.util.Log
import com.peterlaurence.trekme.core.map.data.MAP_ROUTES_DIRECTORY
import com.peterlaurence.trekme.core.map.data.MAP_ROUTE_INFO_FILENAME
import com.peterlaurence.trekme.core.map.data.MAP_ROUTE_MARKERS_FILENAME
import com.peterlaurence.trekme.core.map.data.mappers.toDomain
import com.peterlaurence.trekme.core.map.data.mappers.toRouteInfoKtx
import com.peterlaurence.trekme.core.map.data.mappers.toRouteKtx
import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.core.map.data.models.RouteInfoKtx
import com.peterlaurence.trekme.core.map.data.models.RouteKtx
import com.peterlaurence.trekme.core.map.domain.dao.RouteDao
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class RouteDaoImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
    private val json: Json
): RouteDao {

    /* Associates a route id and the directory name of the serialized route */
    private val routeDirNameForId = mutableMapOf<String, String>()

    override suspend fun importRoutes(map: Map) = withContext(ioDispatcher) {
        routeDirNameForId.clear()
        val directory = (map as? MapFileBased)?.folder ?: return@withContext
        val dir = getOrCreateDirectory(directory, MAP_ROUTES_DIRECTORY) ?: return@withContext
        val routes = getRoutes(dir)

        /* Set the routes on the main thread */
        // TODO: switching to main thread is probably useless here (because of atomic update)
        withContext(mainDispatcher) {
            map.routes.update { routes }
        }
    }

    override suspend fun saveNewRoute(map: Map, route: Route): Unit = withContext(ioDispatcher) {
        runCatching {
            val directory = (map as? MapFileBased)?.folder ?: return@withContext
            val dir =
                getOrCreateDirectory(directory, MAP_ROUTES_DIRECTORY) ?: return@withContext
            routeDirNameForId[route.id] = route.id
            val routeKtx = route.toRouteKtx()
            val routeInfoKtx = route.toRouteInfoKtx()
            serializeRoute(dir, route.id, routeKtx, routeInfoKtx)
        }
    }

    override suspend fun saveRouteInfo(map: Map, route: Route): Unit = withContext(ioDispatcher) {
        runCatching {
            val directory = (map as? MapFileBased)?.folder ?: return@withContext
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
        val directory = (map as? MapFileBased)?.folder ?: return@withContext
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
                    json.decodeFromString(it)
                }
            }.getOrNull()

            if (routeInfoKtx?.id in ids) {
                runCatching {
                    dir.deleteRecursively()
                }
            }
        }
    }

    private suspend fun deleteRouteUsingDirName(map: Map, route: Route) =
        withContext(ioDispatcher) {
            val directory = (map as? MapFileBased)?.folder ?: return@withContext
            val root = File(directory, MAP_ROUTES_DIRECTORY)
            runCatching {
                routeDirNameForId[route.id]?.also { routeDirName ->
                    with(File(root, routeDirName)) {
                        if (exists()) deleteRecursively()
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
                    json.decodeFromString(it)
                }
            }.getOrNull() ?: return@mapNotNull null

            val markersFile = File(dir, MAP_ROUTE_MARKERS_FILENAME)
            val routeMarkerKtx = runCatching<RouteKtx> {
                FileUtils.getStringFromFile(markersFile).let {
                    json.decodeFromString(it)
                }
            }.getOrNull() ?: return@mapNotNull null

            Route(
                id = routeInfoKtx.id,
                initialName = routeInfoKtx.name,
                initialColor = routeInfoKtx.color,
                elevationTrusted = routeInfoKtx.elevationTrusted,
                initialVisibility = routeInfoKtx.visible,
                initialMarkers = routeMarkerKtx.markers.map {
                    it.toDomain()
                }.toMutableList()
            ).also {
                routeDirNameForId[it.id] = dir.name
            }
        }.distinctBy {
            // Protect against copy-paste of a route folder
            it.id
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
        val routeInfoKtxJson = json.encodeToString(routeInfoKtx)
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
        val routeKtsJson = json.encodeToString(routeKtx)
        FileUtils.writeToFile(routeKtsJson, routeMarkersFile)
    }
}

private const val TAG = "RouteDao"