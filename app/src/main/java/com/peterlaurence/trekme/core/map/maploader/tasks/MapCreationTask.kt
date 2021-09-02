package com.peterlaurence.trekme.core.map.maploader.tasks

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.entity.MapGson
import com.peterlaurence.trekme.util.FileUtils
import java.io.File

private const val MAX_RECURSION_DEPTH = 6
private const val TAG = "MapCreationTask"

/**
 * Search for maps on all provided directories.
 * Parses the json files to, e.g, process calibration information.
 *
 * @author P.Laurence on 30/04/2017 -- converted to Kotlin on 05/05/2019
 */
fun mapCreationTask(mGson: Gson, mapFileName: String, vararg dirs: File): List<Map> {
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

            /* Map creation */
            val map = if (mapGson.thumbnail == null)
                Map(mapGson, f, null)
            else
                Map(mapGson, f, File(f.parent, mapGson.thumbnail))

            /* Calibration */
            map.calibrate()

            mapList.add(map)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, e.message, e)
        } catch (e: NullPointerException) {
            Log.e(TAG, e.message, e)
        }
    }

    return mapList
}
