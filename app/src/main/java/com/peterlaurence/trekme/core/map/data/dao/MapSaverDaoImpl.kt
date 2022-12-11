package com.peterlaurence.trekme.core.map.data.dao

import android.util.Log
import com.google.gson.Gson
import com.peterlaurence.trekme.core.map.data.MAP_FILENAME
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import com.peterlaurence.trekme.core.map.data.mappers.toMarkerGson
import com.peterlaurence.trekme.util.writeToFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

class MapSaverDaoImpl (
    private val fileBasedMapRegistry: FileBasedMapRegistry,
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val gson: Gson
): MapSaverDao {
    override suspend fun save(map: Map) {
        val jsonString = withContext(mainDispatcher) {
            gson.toJson(map.configSnapshot.toMarkerGson())
        }

        val rootFolder = fileBasedMapRegistry.getRootFolder(map.id) ?: return
        withContext(ioDispatcher) {
            val configFile = File(rootFolder, MAP_FILENAME)
            writeToFile(jsonString, configFile) {
                Log.e(this::class.java.name, "Error while saving the map")
            }
        }
    }
}