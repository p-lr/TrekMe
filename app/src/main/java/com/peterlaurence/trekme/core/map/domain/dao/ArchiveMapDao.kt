package com.peterlaurence.trekme.core.map.domain.dao

import android.net.Uri
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.util.ZipProgressionListener

interface ArchiveMapDao {
    suspend fun archiveMap(map: Map, listener: ZipProgressionListener, uri: Uri)
}