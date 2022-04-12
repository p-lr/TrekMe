package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.dao.MapDeleteDao
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import com.peterlaurence.trekme.core.map.Map
import javax.inject.Inject

class DeleteMapInteractor @Inject constructor(
    private val mapDeleteDao: MapDeleteDao,
    private val mapRepository: MapRepository,
) {
    suspend fun deleteMap(map: Map) {
        mapRepository.deleteMap(map)
        mapDeleteDao.deleteMap(map)
    }
}