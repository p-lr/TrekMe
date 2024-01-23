package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.core.map.data.models.MapRepairKtx
import com.peterlaurence.trekme.core.map.domain.dao.MissingTilesCountDao
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.writeToFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * This is a file based implementation of [MissingTilesCountDao].
 */
class MissingTilesCountDaoImpl(
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
) : MissingTilesCountDao {

    override suspend fun setMissingTilesCount(map: Map, count: Long): Boolean {
        withContext(mainDispatcher) {
            map.missingTilesCount.value = count
        }
        return withContext(ioDispatcher) {
            val directory = (map as? MapFileBased)?.folder ?: return@withContext false
            val repairFile = File(directory, repairFileName)
            if (!repairFile.exists()) {
                repairFile.createNewFile()
            }
            val repairData = MapRepairKtx(count)
            val str = json.encodeToString(repairData)
            writeToFile(str, repairFile).isSuccess
        }
    }

    override suspend fun loadMissingTilesCount(map: Map): Long? = withContext(ioDispatcher) {
        val directory = (map as? MapFileBased)?.folder ?: return@withContext null
        val repairFile = File(directory, repairFileName)
        if (!repairFile.exists()) {
            return@withContext null
        }

        val missingTilesCount = runCatching<MapRepairKtx> {
            FileUtils.getStringFromFile(repairFile).let {
                json.decodeFromString(it)
            }
        }.map {
            it.missingTilesCount
        }.getOrNull()

        if (missingTilesCount != null) {
            map.missingTilesCount.update { missingTilesCount }
        }

        missingTilesCount
    }
}

