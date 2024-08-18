package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.core.map.data.models.MapUpdateKtx
import com.peterlaurence.trekme.core.map.domain.dao.MapUpdateDataDao
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.utils.deleteDownloadPendingFile
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.writeToFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * This is a file based implementation of [MapUpdateDataDao].
 */
class MapUpdateDataDaoImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
) : MapUpdateDataDao {

    override suspend fun setNewDownloadData(map: Map, missingTilesCount: Long) {
        val success = withContext(ioDispatcher) {
            val directory = (map as? MapFileBased)?.folder ?: return@withContext false
            removeDownloadPendingFlag(map)

            val updateData = MapUpdateKtx(missingTilesCount = missingTilesCount)
            val str = json.encodeToString(updateData)
            val repairFile = File(directory, updateFileName)
            if (!repairFile.exists()) {
                repairFile.createNewFile()
            }
            writeToFile(str, repairFile).isSuccess
        }
        if (success) {
            map.missingTilesCount.update { missingTilesCount }
        }
    }

    override suspend fun setRepairData(map: Map, missingTilesCount: Long, date: Long) {
        val success = withContext(ioDispatcher) {
            val directory = (map as? MapFileBased)?.folder ?: return@withContext false
            removeDownloadPendingFlag(map)

            val repairFile = File(directory, updateFileName)

            val mapUpdateKtx = runCatching<MapUpdateKtx> {
                FileUtils.getStringFromFile(repairFile).let {
                    json.decodeFromString(it)
                }
            }.getOrNull() ?: MapUpdateKtx()

            val updateData = mapUpdateKtx.copy(
                missingTilesCount = missingTilesCount,
                lastRepairDate = date
            )
            val str = json.encodeToString(updateData)
            if (!repairFile.exists()) {
                repairFile.createNewFile()
            }
            writeToFile(str, repairFile).isSuccess
        }
        if (success) {
            map.missingTilesCount.update { missingTilesCount }
            map.lastRepairDate.update { date }
        }
    }

    override suspend fun setUpdateData(map: Map, missingTilesCount: Long, date: Long) {
        val success = withContext(ioDispatcher) {
            val directory = (map as? MapFileBased)?.folder ?: return@withContext false
            removeDownloadPendingFlag(map)

            val updateFile = File(directory, updateFileName)

            val mapUpdateKtx = runCatching<MapUpdateKtx> {
                FileUtils.getStringFromFile(updateFile).let {
                    json.decodeFromString(it)
                }
            }.getOrNull() ?: MapUpdateKtx()

            val updateData = mapUpdateKtx.copy(
                missingTilesCount = missingTilesCount,
                lastUpdateDate = date
            )
            val str = json.encodeToString(updateData)
            if (!updateFile.exists()) {
                updateFile.createNewFile()
            }
            writeToFile(str, updateFile).isSuccess
        }
        if (success) {
            map.missingTilesCount.update { missingTilesCount }
            map.lastUpdateDate.update { date }
        }
    }

    override suspend fun loadMapUpdateData(map: Map): Unit = withContext(ioDispatcher) {
        val directory = (map as? MapFileBased)?.folder ?: return@withContext
        val repairFile = File(directory, updateFileName)
        if (!repairFile.exists()) {
            return@withContext
        }

        val mapUpdateKtx = runCatching<MapUpdateKtx> {
            FileUtils.getStringFromFile(repairFile).let {
                json.decodeFromString(it)
            }
        }.getOrNull()

        if (mapUpdateKtx != null) {
            map.missingTilesCount.update { mapUpdateKtx.missingTilesCount }
            map.lastRepairDate.update { mapUpdateKtx.lastRepairDate }
            map.lastUpdateDate.update { mapUpdateKtx.lastUpdateDate }
        }
    }

    private suspend fun removeDownloadPendingFlag(map: MapFileBased) {
        val directory = (map as? MapFileBased)?.folder ?: return
        deleteDownloadPendingFile(directory)
        map.isDownloadPending.value = false
    }
}

