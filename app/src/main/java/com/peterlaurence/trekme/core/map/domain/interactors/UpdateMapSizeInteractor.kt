package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapSizeComputeDao
import javax.inject.Inject

class UpdateMapSizeInteractor @Inject constructor(
    private val mapSizeComputeDao: MapSizeComputeDao,
    private val saveMapInteractor: SaveMapInteractor
) {
    suspend fun updateMapSize(map: Map) : Result<Long> {
        return mapSizeComputeDao.computeMapSize(map).onSuccess {
            map.sizeInBytes.value = it
            saveMapInteractor.saveMap(map)
        }
    }
}