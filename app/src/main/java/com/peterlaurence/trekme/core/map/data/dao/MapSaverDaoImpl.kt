package com.peterlaurence.trekme.core.map.data.dao

import android.util.Log
import com.google.gson.Gson
import com.peterlaurence.trekme.core.map.data.MAP_FILENAME
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import com.peterlaurence.trekme.core.map.data.mappers.toMapGson
import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.util.writeToFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

class MapSaverDaoImpl (
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val gson: Gson
): MapSaverDao {
    override suspend fun save(map: Map) {
        val jsonString = withContext(mainDispatcher) {
            val mapGson = map.configSnapshot.toMapGson()
            mapGson.sizeInBytes = map.sizeInBytes.value
            gson.toJson(mapGson)
        }

        val rootFolder = (map as? MapFileBased)?.folder ?: return
        withContext(ioDispatcher) {
            val configFile = File(rootFolder, MAP_FILENAME)
            writeToFile(jsonString, configFile) {
                Log.e(this::class.java.name, "Error while saving the map")
            }
        }
    }
}