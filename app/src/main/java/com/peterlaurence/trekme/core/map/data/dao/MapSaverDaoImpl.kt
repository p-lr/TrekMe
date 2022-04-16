package com.peterlaurence.trekme.core.map.data.dao

import android.util.Log
import com.google.gson.Gson
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import com.peterlaurence.trekme.core.map.mappers.toEntity
import com.peterlaurence.trekme.util.writeToFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class MapSaverDaoImpl (
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val gson: Gson
): MapSaverDao {
    override suspend fun save(map: Map) {
        val jsonString = withContext(mainDispatcher) {
            gson.toJson(map.configSnapshot.toEntity())
        }

        withContext(ioDispatcher) {
            val configFile = map.configFile
            writeToFile(jsonString, configFile) {
                Log.e(this::class.java.name, "Error while saving the map")
            }
        }
    }
}