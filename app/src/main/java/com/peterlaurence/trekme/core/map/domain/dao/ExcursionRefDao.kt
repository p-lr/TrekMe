package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map

interface ExcursionRefDao {
    suspend fun importExcursionRefs(map: Map)
    suspend fun saveExcursionRef(map: Map, ref: ExcursionRef)
    suspend fun removeExcursionRef(map: Map, ref: ExcursionRef)
}