package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map

interface MapRenameDao {
    suspend fun renameMap(map: Map, newName: String)
}