package com.peterlaurence.trekme.core.map.data.dao

import android.util.Log
import com.peterlaurence.trekme.core.map.data.MAP_MARKER_FILENAME
import com.peterlaurence.trekme.core.map.data.mappers.toDomain
import com.peterlaurence.trekme.core.map.data.mappers.toMarkerKtx
import com.peterlaurence.trekme.core.map.data.models.MarkerListKtx
import com.peterlaurence.trekme.core.map.domain.dao.MarkersDao
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


class MarkersDaoImpl(
    private val fileBasedMapRegistry: FileBasedMapRegistry,
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
) : MarkersDao {

    /**
     * Get markers by reading the markers.json file.
     */
    override suspend fun getMarkersForMap(map: Map): Boolean {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return false

        val markerList = withContext(ioDispatcher) {
            val markerFile = File(directory, MAP_MARKER_FILENAME)
            if (!markerFile.exists()) return@withContext null

            runCatching<MarkerListKtx> {
                FileUtils.getStringFromFile(markerFile).let {
                    json.decodeFromString(it)
                }
            }.map {
                it.markers.map { b -> b.toDomain() }
            }.getOrNull()
        } ?: return false

        withContext(mainDispatcher) {
            map.markers.value = markerList
        }
        return true
    }

    /**
     * Re-writes the markers.json file.
     */
    override suspend fun saveMarkers(map: Map) {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return

        withContext(ioDispatcher) {
            runCatching {
                val markers = map.markers.value.map { it.toMarkerKtx() }

                val markerFile = File(directory, MAP_MARKER_FILENAME).also {
                    if (!it.createNewFile()) {
                        Log.e(TAG, "Error while creating $MAP_MARKER_FILENAME")
                    }
                }

                val markerStr = json.encodeToString(MarkerListKtx(markers))
                FileUtils.writeToFile(markerStr, markerFile)
            }
        }
    }
}

private const val TAG = "MarkersDao"