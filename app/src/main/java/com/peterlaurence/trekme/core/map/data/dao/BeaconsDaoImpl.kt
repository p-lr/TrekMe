package com.peterlaurence.trekme.core.map.data.dao

import android.util.Log
import com.peterlaurence.trekme.core.map.data.MAP_BEACON_FILENAME
import com.peterlaurence.trekme.core.map.data.mappers.toBeaconKtx
import com.peterlaurence.trekme.core.map.data.mappers.toDomain
import com.peterlaurence.trekme.core.map.data.models.BeaconListKtx
import com.peterlaurence.trekme.core.map.domain.dao.BeaconDao
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class BeaconsDaoImpl(
    private val fileBasedMapRegistry: FileBasedMapRegistry,
    private val ioDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
    private val json: Json
) : BeaconDao {
    override suspend fun getBeaconsForMap(map: Map): Boolean {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return false
        val beaconList = withContext(ioDispatcher) {
            val beaconFile = File(directory, MAP_BEACON_FILENAME)
            if (!beaconFile.exists()) return@withContext null

            runCatching<BeaconListKtx> {
                FileUtils.getStringFromFile(beaconFile).let {
                    json.decodeFromString(it)
                }
            }.map {
                it.beacons.map { b -> b.toDomain() }
            }.getOrNull()
        } ?: return false

        withContext(mainDispatcher) {
            map.beacons.value = beaconList
        }
        return true
    }

    override suspend fun saveBeacons(map: Map) {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return

        withContext(ioDispatcher) {
            runCatching {
                val beacons = map.beacons.value.map { it.toBeaconKtx() }

                val beaconFile = File(directory, MAP_BEACON_FILENAME).also {
                    if (!it.createNewFile()) {
                        Log.e(TAG, "Error while creating $MAP_BEACON_FILENAME")
                    }
                }

                val beaconStr = json.encodeToString(BeaconListKtx(beacons))
                FileUtils.writeToFile(beaconStr, beaconFile)
            }
        }
    }
}

private const val TAG = "RouBeaconsDaoteDao"