package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.dao.MapLoaderDao
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import java.io.File
import javax.inject.Inject

class UpdateMapsInteractor @Inject constructor(
    private val mapLoaderDao: MapLoaderDao,
    private val mapRepository: MapRepository
) {

    suspend fun updateMaps(dirs: List<File>) {
        val maps = mapLoaderDao.loadMaps(dirs)

        mapRepository.addMaps(maps)
    }
}