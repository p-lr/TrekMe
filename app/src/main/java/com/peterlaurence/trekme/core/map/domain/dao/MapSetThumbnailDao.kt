package com.peterlaurence.trekme.core.map.domain.dao

import android.net.Uri
import com.peterlaurence.trekme.core.map.domain.models.Map

interface MapSetThumbnailDao {
    suspend fun setThumbnail(map: Map, uri: Uri): Result<Map>
}