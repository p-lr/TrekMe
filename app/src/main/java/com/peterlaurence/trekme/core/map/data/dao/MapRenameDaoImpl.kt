package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapRenameDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class MapRenameDaoImpl(
    private val mainDispatcher: CoroutineDispatcher,
    private val mapSaverDao: MapSaverDao
) : MapRenameDao {

    /**
     * The renaming is done by updating the name and saving the json file right after.
     */
    override suspend fun renameMap(map: Map, newName: String) = withContext(mainDispatcher) {
        map.name.value = newName
        mapSaverDao.save(map)
    }
}