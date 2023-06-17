package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map

interface ExcursionRefDao {
    suspend fun importExcursionRefs(map: Map)
    suspend fun saveExcursionRef(map: Map, ref: ExcursionRef)
    suspend fun removeExcursionRef(ref: ExcursionRef)
    suspend fun createExcursionRef(map: Map, excursion: Excursion): ExcursionRef?
}