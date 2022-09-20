package com.peterlaurence.trekme.core.map.domain.interactors

import android.net.Uri
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapSetThumbnailDao
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import javax.inject.Inject

class SetMapThumbnailInteractor @Inject constructor(
    private val mapSetThumbnailDao: MapSetThumbnailDao,
    private val mapRepository: MapRepository,
) {
    suspend fun setMapThumbnail(map: Map, uri: Uri): Result<Map> {
        return mapSetThumbnailDao.setThumbnail(map, uri).onSuccess { newMap ->
            mapRepository.notifyUpdate(map, newMap)
        }
    }
}