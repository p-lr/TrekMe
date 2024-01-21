package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.UpdateMapSizeInBytesDao
import javax.inject.Inject

class UpdateMapSizeInteractor @Inject constructor(
    private val updateMapSizeInBytesDao: UpdateMapSizeInBytesDao,
) {
    suspend fun updateMapSize(map: Map) : Result<Long> {
        return updateMapSizeInBytesDao.updateMapSize(map)
    }
}