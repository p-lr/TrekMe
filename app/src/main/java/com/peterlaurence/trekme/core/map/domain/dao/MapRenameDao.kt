package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.Map

interface MapRenameDao {
    suspend fun renameMap(map: Map, newName: String): Boolean
}