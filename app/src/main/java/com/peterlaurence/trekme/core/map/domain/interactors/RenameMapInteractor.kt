package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapRenameDao
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import javax.inject.Inject

class RenameMapInteractor @Inject constructor(
    private val mapRenameDao: MapRenameDao,
) {
    suspend fun renameMap(map: Map, newName: String) {
        mapRenameDao.renameMap(map, newName)
    }
}