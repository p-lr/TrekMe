package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapRenameDao
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import javax.inject.Inject

class RenameMapInteractor @Inject constructor(
    private val mapRenameDao: MapRenameDao,
    private val mapRepository: MapRepository,
) {
    suspend fun renameMap(map: Map, newName: String) {
        mapRenameDao.renameMap(map, newName).onSuccess { newMap ->
            mapRepository.notifyUpdate(map, newMap)
        }
    }
}