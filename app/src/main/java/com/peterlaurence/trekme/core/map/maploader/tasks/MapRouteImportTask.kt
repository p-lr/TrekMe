package com.peterlaurence.trekme.core.map.maploader.tasks

import android.util.Log
import com.google.gson.Gson
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.util.FileUtils
import java.io.File


/**
 * A file named 'routes.json' (also referred as track file) is expected at the same level of the
 * 'map.json' configuration file. If there is no track file, this means that the map has no routes.
 *
 * This should be called off UI thread.
 *
 * @return A [RouteGson] instance if it succeeded, null otherwise.
 *
 * @author peterLaurence on 13/05/17 -- Converted to Kotlin on 17/02/2019
 */
fun mapRouteImportTask(map: Map, gson: Gson): RouteGson? {
    val routeFile = File(map.directory, MapLoader.MAP_ROUTE_FILE_NAME)
    if (!routeFile.exists()) return null

    return try {
        val jsonString = FileUtils.getStringFromFile(routeFile)
        val routeGson = gson.fromJson(jsonString, RouteGson::class.java)
        routeGson
    } catch (e: Exception) {
        /* Error while decoding the json file */
        Log.e(TAG, e.message, e)
        null
    }
}

private const val TAG = "MapRouteImportTask"
