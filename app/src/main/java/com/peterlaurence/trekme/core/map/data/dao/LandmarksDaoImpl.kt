package com.peterlaurence.trekme.core.map.data.dao

import android.util.Log
import com.peterlaurence.trekme.core.map.data.MAP_LANDMARK_FILENAME
import com.peterlaurence.trekme.core.map.data.mappers.toDomain
import com.peterlaurence.trekme.core.map.data.mappers.toLandmarkKtx
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.data.models.LandmarkListKtx
import com.peterlaurence.trekme.core.map.domain.dao.LandmarksDao
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class LandmarksDaoImpl (
    private val fileBasedMapRegistry: FileBasedMapRegistry,
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
) : LandmarksDao {
    /**
     * Reads the landmarks.json file off UI thread to get the list of landmarks.
     * Right after, if the result is not null, updates the [Map] on the main thread.
     */
    override suspend fun getLandmarksForMap(map: Map): Boolean {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return false
        val landmarkList = withContext(ioDispatcher) {
            val landmarkFile = File(directory, MAP_LANDMARK_FILENAME)
            if (!landmarkFile.exists()) return@withContext null

            runCatching<LandmarkListKtx> {
                FileUtils.getStringFromFile(landmarkFile).let {
                    json.decodeFromString(it)
                }
            }.map {
                it.landmarks.map { l -> l.toDomain() }
            }.getOrNull()
        } ?: return false

        withContext(mainDispatcher) {
            map.landmarks.value = landmarkList
        }
        return true
    }

    /**
     * Re-writes the landmarks.json file.
     */
    override suspend fun saveLandmarks(map: Map) {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return

        withContext(ioDispatcher) {
            runCatching {
                val landmarks = map.landmarks.value.map { it.toLandmarkKtx() }

                val landmarkFile = File(directory, MAP_LANDMARK_FILENAME).also {
                    if (!it.createNewFile()) {
                        Log.e(TAG, "Error while creating $MAP_LANDMARK_FILENAME")
                    }
                }

                val landmarkStr = json.encodeToString(LandmarkListKtx(landmarks))
                FileUtils.writeToFile(landmarkStr, landmarkFile)
            }
        }
    }
}

private const val TAG = "LandmarksDao"