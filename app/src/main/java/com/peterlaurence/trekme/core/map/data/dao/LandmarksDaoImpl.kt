package com.peterlaurence.trekme.core.map.data.dao

import android.util.Log
import com.google.gson.Gson
import com.peterlaurence.trekme.core.map.MAP_LANDMARK_FILENAME
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.data.models.LandmarkGson
import com.peterlaurence.trekme.core.map.domain.dao.LandmarksDao
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.writeToFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

class LandmarksDaoImpl (
    private val fileBasedMapRegistry: FileBasedMapRegistry,
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val gson: Gson
) : LandmarksDao {
    /**
     * Reads the landmarks.json file off UI thread to get a nullable instance of [LandmarkGson].
     * Right after, if the result is not null, updates the [Map] on the main thread.
     */
    override suspend fun getLandmarksForMap(map: Map): Boolean {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return false
        val landmarkGson = withContext(ioDispatcher) {
            val landmarkFile = File(directory, MAP_LANDMARK_FILENAME)
            if (!landmarkFile.exists()) return@withContext null

            try {
                val jsonString = FileUtils.getStringFromFile(landmarkFile)
                gson.fromJson(jsonString, LandmarkGson::class.java)
            } catch (e: Exception) {
                Log.e(this.javaClass.name, e.message, e)
                null
            }
        } ?: return false

        withContext(mainDispatcher) {
            map.setLandmarks(landmarkGson.landmarks)
        }
        return true
    }

    /**
     * Re-writes the landmarks.json file.
     */
    override suspend fun saveLandmarks(map: Map) = withContext(mainDispatcher) {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return@withContext
        val jsonString = gson.toJson(LandmarkGson(map.landmarks ?: listOf()))

        withContext(ioDispatcher) {
            val landmarkFile = File(directory, MAP_LANDMARK_FILENAME)
            writeToFile(jsonString, landmarkFile) {
                Log.e(this.javaClass.name, "Error while saving the landmarks")
            }
        }
    }
}