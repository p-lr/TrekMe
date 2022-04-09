package com.peterlaurence.trekme.core.map.maploader.tasks

import android.util.Log
import com.google.gson.Gson
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.data.LandmarkGson
import com.peterlaurence.trekme.util.FileUtils
import java.io.File

/**
 *  A file named 'landmarks.json' may exist at the same level of the 'map.json' configuration file.
 *  If there is no landmark file, this means that the map has no landmarks.
 *
 * This should be called off UI thread.
 * @author P.Laurence on 23/02/2019
 */
fun mapLandmarkImportTask(map: Map, gson: Gson, fileName: String): LandmarkGson? {
    val landmarkFile = File(map.directory, fileName)
    if (!landmarkFile.exists()) return null

    return try {
        val jsonString = FileUtils.getStringFromFile(landmarkFile)
        val landmarkGson = gson.fromJson(jsonString, LandmarkGson::class.java)
        landmarkGson
    } catch (e: Exception) {
        Log.e(TAG, e.message, e)
        null
    }
}

private const val TAG = "MapLandmarkImportTask"