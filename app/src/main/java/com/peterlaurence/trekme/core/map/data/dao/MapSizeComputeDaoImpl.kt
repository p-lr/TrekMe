package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapSizeComputeDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MapSizeComputeDaoImpl(
    private val fileBasedMapRegistry: FileBasedMapRegistry,
    private val defaultDispatcher: CoroutineDispatcher
) : MapSizeComputeDao {

    override suspend fun computeMapSize(map: Map): Result<Long> = runCatching {
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: throw Exception("No map for this id")
        withContext(defaultDispatcher) {
            val size = directory.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            withContext(Dispatchers.Main) {
                map.setSizeInBytes(size)
            }
            size
        }
    }
}