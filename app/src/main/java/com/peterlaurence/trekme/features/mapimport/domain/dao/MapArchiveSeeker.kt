package com.peterlaurence.trekme.features.mapimport.domain.dao

import android.net.Uri
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchive

interface MapArchiveSeeker {
    suspend fun seek(uri: Uri): List<MapArchive>
}