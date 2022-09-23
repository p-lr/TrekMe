package com.peterlaurence.trekme.core.map.data.dao

import android.util.Log
import com.google.gson.Gson
import com.peterlaurence.trekme.core.map.data.MAP_MARKER_FILENAME
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.data.models.MarkerGson
import com.peterlaurence.trekme.core.map.domain.dao.MarkersDao
import com.peterlaurence.trekme.core.map.data.mappers.toDomain
import com.peterlaurence.trekme.core.map.data.mappers.toEntity
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.writeToFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File


class MarkersDaoImpl(
    private val fileBasedMapRegistry: FileBasedMapRegistry,
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val gson: Gson
) : MarkersDao {

    /**
     * Get markers by reading the markers.json file.
     */
    override suspend fun getMarkersForMap(map: Map): Boolean = withContext(ioDispatcher) {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return@withContext false
        val markerFile = File(directory, MAP_MARKER_FILENAME)
        if (!markerFile.exists()) return@withContext false

        val markerGson = runCatching {
            val jsonString = FileUtils.getStringFromFile(markerFile)
            gson.fromJson(jsonString, MarkerGson::class.java)
        }.onFailure {
            Log.e(this.javaClass.name, it.message, it)
        }.getOrNull()

        /* Update the map on the main thread */
        withContext(mainDispatcher) {
            if (markerGson != null) {
                map.setMarkers(markerGson.markers.map { it.toDomain() })
                true
            } else false
        }
    }

    /**
     * Re-writes the markers.json file.
     */
    override suspend fun saveMarkers(map: Map) = withContext(mainDispatcher) {
        val markerGson =
            MarkerGson().apply { markers = map.markers?.map { it.toEntity() } ?: listOf() }
        val jsonString = gson.toJson(markerGson)

        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return@withContext
        withContext(ioDispatcher) {
            val markerFile = File(directory, MAP_MARKER_FILENAME)
            writeToFile(jsonString, markerFile) {
                Log.e(this.javaClass.name, "Error while saving the markers")
            }
        }
    }
}