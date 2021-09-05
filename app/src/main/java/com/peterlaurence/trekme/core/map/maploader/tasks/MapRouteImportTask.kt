package com.peterlaurence.trekme.core.map.maploader.tasks

import android.util.Log
import com.google.gson.Gson
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Route
import com.peterlaurence.trekme.core.map.entity.RouteGson
import com.peterlaurence.trekme.core.map.mappers.toDomain
import com.peterlaurence.trekme.util.FileUtils
import java.io.File


/**
 * A file named 'routes.json' (also referred as track file) is expected at the same level of the
 * 'map.json' configuration file. If there is no track file, this means that the map has no routes.
 *
 * This should be called off UI thread.
 *
 * @return A list of [Route] if it succeeded, null otherwise.
 *
 * @author P.Laurence on 13/05/17 -- Converted to Kotlin on 17/02/2019
 */
fun mapRouteImportTask(map: Map, gson: Gson, fileName: String): List<Route>? {
    val routeFile = File(map.directory, fileName)
    if (!routeFile.exists()) return null

    return try {
        val jsonString = FileUtils.getStringFromFile(routeFile)
        val routeGson = gson.fromJson(jsonString, RouteGson::class.java)
        routeGson.routes.map { it.toDomain() }
    } catch (e: Exception) {
        /* Error while decoding the json file */
        Log.e(TAG, e.message, e)
        null
    }
}

private const val TAG = "MapRouteImportTask"
