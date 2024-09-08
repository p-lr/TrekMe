package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map

interface ExcursionRefDao {
    suspend fun importExcursionRefs(map: Map, excursionProvider: suspend (String) -> Excursion?)
    suspend fun saveExcursionRef(map: Map, ref: ExcursionRef)
    suspend fun removeExcursionRef(map: Map, ref: ExcursionRef)
    suspend fun removeExcursionRef(map: Map, excursionRefId: String)
    suspend fun createExcursionRef(map: Map, excursion: Excursion): ExcursionRef?
}