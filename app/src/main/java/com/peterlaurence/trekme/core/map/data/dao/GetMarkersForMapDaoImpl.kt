package com.peterlaurence.trekme.core.map.data.dao

import android.util.Log
import com.google.gson.Gson
import com.peterlaurence.trekme.core.map.MAP_MARKER_FILENAME
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.data.MarkerGson
import com.peterlaurence.trekme.core.map.domain.dao.GetMarkersForMapDao
import com.peterlaurence.trekme.core.map.mappers.toDomain
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

/**
 * This interactor get markers by reading the markers.json file.
 */
class GetMarkersForMapDaoImpl(
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val gson: Gson
) : GetMarkersForMapDao {
    override suspend fun getMarkersForMap(map: Map): Boolean = withContext(ioDispatcher) {
        val markerFile = File(map.directory, MAP_MARKER_FILENAME)
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
}