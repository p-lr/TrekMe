package com.peterlaurence.trekme.core.map.domain.interactors

import android.net.Uri
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapSetThumbnailDao
import javax.inject.Inject

class SetMapThumbnailInteractor @Inject constructor(
    private val mapSetThumbnailDao: MapSetThumbnailDao,
) {
    suspend fun setMapThumbnail(map: Map, uri: Uri): Boolean {
        return mapSetThumbnailDao.setThumbnail(map, uri)
    }
}