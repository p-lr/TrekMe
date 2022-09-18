package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapRenameDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

class MapRenameDaoImpl(
    private val fileBasedMapRegistry: FileBasedMapRegistry,
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val mapSaverDao: MapSaverDao
) : MapRenameDao {

    /**
     * The renaming is done by:
     * - Changing the name of the folder containing the map
     * - Updating the name and saving the json file right after
     */
    override suspend fun renameMap(map: Map, newName: String): Result<Map> = withContext(mainDispatcher) {
        /* Update the containing folder */
        val directory = fileBasedMapRegistry.getRootFolder(map.id) ?: return@withContext Result.failure(Exception("No parent folder"))
        val newDirectory = withContext(ioDispatcher) {
            runCatching {
                val dir = File(directory.parentFile, newName)
                directory.renameTo(dir)
                dir
            }.getOrElse {
                directory
            }
        }

        /* Update the name and directory */
        val newMap = map.copy(config = map.configSnapshot.copy(name = newName), configFile = File(newDirectory, map.configFile.name))
        fileBasedMapRegistry.setRootFolder(newMap.id, newDirectory)

        mapSaverDao.save(newMap)

        Result.success(newMap)
    }
}