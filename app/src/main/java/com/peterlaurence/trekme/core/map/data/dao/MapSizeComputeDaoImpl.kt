package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapSizeComputeDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MapSizeComputeDaoImpl(
    private val defaultDispatcher: CoroutineDispatcher
) : MapSizeComputeDao {

    override suspend fun computeMapSize(map: Map): Result<Long> = runCatching {
        withContext(defaultDispatcher) {
            val size = map.directory!!.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            withContext(Dispatchers.Main) {
                map.setSizeInBytes(size)
            }
            size
        }
    }
}