package com.peterlaurence.trekme.core.map.data.dao

import android.os.StatFs
import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.core.map.data.models.MapPropertiesKtx
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.UpdateMapSizeInBytesDao
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.writeToFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class UpdateMapSizeInBytesDaoImpl(
    private val json: Json,
    private val defaultDispatcher: CoroutineDispatcher
) : UpdateMapSizeInBytesDao {

    /**
     * Computes logical and physical sizes. The physical size takes into account the block size.
     * Most devices have a block size of 4096 for internal memory. In this case, the difference
     * between logical and physical sizes is insignificant for maps (as tile images are roughly 3-4
     * times the size of a block).
     * SD cards, however, can have much bigger blocks...
     */
    override suspend fun updateMapSize(map: Map): Result<Long> = runCatching {
        val directory = (map as? MapFileBased)?.folder ?: throw Exception("No map for this id")
        withContext(defaultDispatcher) {
            val statFs = StatFs(directory.absolutePath)
            val blockSize = statFs.blockSizeLong

            var logicalSize = 0L
            var physicalSize = 0L

            directory.walkTopDown().forEach {

                if (it.isFile) {
                    val l = it.length()
                    logicalSize += l
                    physicalSize += (l / blockSize + 1) * blockSize
                }
            }

            // TODO: maybe do something with the physical size

            withContext(Dispatchers.Main) {
                map.sizeInBytes.value = logicalSize
            }

            withContext(Dispatchers.IO) {
                val propertiesFile = File(directory, propertiesFileName)
                val existingProperties = if (!propertiesFile.exists()) {
                    propertiesFile.createNewFile()
                    null
                } else {
                    runCatching<MapPropertiesKtx> {
                        FileUtils.getStringFromFile(propertiesFile).let {
                            json.decodeFromString(it)
                        }
                    }.getOrNull()
                } ?: MapPropertiesKtx()
                val mapProperties = existingProperties.copy(sizeInBytes = logicalSize)
                val str = json.encodeToString(mapProperties)
                writeToFile(str, propertiesFile)
            }

            logicalSize
        }
    }
}