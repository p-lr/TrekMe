package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.Map
import java.io.File

interface MapLoaderDao {
    suspend fun loadMaps(dirs: List<File>): List<Map>
}