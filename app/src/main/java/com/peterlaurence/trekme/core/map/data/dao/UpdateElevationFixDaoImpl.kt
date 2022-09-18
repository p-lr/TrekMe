package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.data.models.MapPropertiesKtx
import com.peterlaurence.trekme.core.map.domain.dao.UpdateElevationFixDao
import com.peterlaurence.trekme.util.writeToFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * This is a file based implementation of [UpdateElevationFixDao].
 */
class UpdateElevationFixDaoImpl(
    private val fileBasedMapRegistry: FileBasedMapRegistry,
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
) : UpdateElevationFixDao {

    override suspend fun setElevationFix(map: Map, fix: Int): Boolean = withContext(ioDispatcher) {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return@withContext false
        val propertiesFile = File(directory, propertiesFileName)
        if (!propertiesFile.exists()) {
            propertiesFile.createNewFile()
        }
        val mapProperties = MapPropertiesKtx(fix)
        val str = json.encodeToString(mapProperties)
        writeToFile(str, propertiesFile).isSuccess
    }
}

