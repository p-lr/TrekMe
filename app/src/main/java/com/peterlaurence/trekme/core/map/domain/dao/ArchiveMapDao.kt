package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.util.ZipProgressionListener
import java.io.OutputStream

interface ArchiveMapDao {
    suspend fun archiveMap(map: Map, listener: ZipProgressionListener, outputStream: OutputStream)
}