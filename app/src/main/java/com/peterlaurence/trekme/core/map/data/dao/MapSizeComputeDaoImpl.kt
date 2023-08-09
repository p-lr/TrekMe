package com.peterlaurence.trekme.core.map.data.dao

import android.os.StatFs
import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapSizeComputeDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MapSizeComputeDaoImpl(
    private val defaultDispatcher: CoroutineDispatcher
) : MapSizeComputeDao {

    /**
     * Computes logical and physical sizes. The physical size takes into account the block size.
     * Most devices have a block size of 4096 for internal memory. In this case, the difference
     * between logical and physical sizes is insignificant for maps (as tile images are roughly 3-4
     * times the size of a block).
     * SD cards, however, can have much bigger blocks...
     */
    override suspend fun computeMapSize(map: Map): Result<Long> = runCatching {
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
            logicalSize
        }
    }
}