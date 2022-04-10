package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapRenameDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

class MapRenameDaoImpl(
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val mapSaverDao: MapSaverDao
) : MapRenameDao {

    /**
     * The renaming is done by:
     * - Updating the name and saving the json file right after
     * - Changing the name of the folder containing the map
     */
    override suspend fun renameMap(map: Map, newName: String): Boolean = withContext(mainDispatcher) {
        /* Update the name */
        map.name = newName
        mapSaverDao.save(map)

        /* Update the containing folder */
        val directory = map.directory ?: return@withContext false
        val newDirectory = File(directory.parentFile, newName)
        withContext(ioDispatcher) {
            runCatching {

                directory.renameTo(newDirectory).also { renameOk ->
                    if (renameOk) {
                        map.directory = newDirectory
                    }
                }
            }.getOrNull() ?: false
        }
    }
}