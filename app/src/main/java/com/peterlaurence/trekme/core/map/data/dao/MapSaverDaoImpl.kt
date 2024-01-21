package com.peterlaurence.trekme.core.map.data.dao

import android.util.Log
import com.peterlaurence.trekme.core.map.data.MAP_FILENAME
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import com.peterlaurence.trekme.core.map.data.mappers.toMapKtx
import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.util.writeToFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

class MapSaverDaoImpl (
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
): MapSaverDao {
    override suspend fun save(map: Map) {
        val jsonString = withContext(mainDispatcher) {
            val mapKtx = map.configSnapshot.toMapKtx()
            json.encodeToString(mapKtx)
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