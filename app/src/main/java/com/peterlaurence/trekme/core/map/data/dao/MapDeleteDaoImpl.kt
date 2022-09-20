package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapDeleteDao
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class MapDeleteDaoImpl(
    private val fileBasedMapRegistry: FileBasedMapRegistry,
    private val ioDispatcher: CoroutineDispatcher
) : MapDeleteDao {
    override suspend fun deleteMap(map: Map) {
        /* Delete the map directory in a separate thread */
        val mapDirectory = fileBasedMapRegistry.getRootFolder(map.id) ?: return
        withContext(ioDispatcher) {
            FileUtils.deleteRecursive(mapDirectory)
        }
    }
}