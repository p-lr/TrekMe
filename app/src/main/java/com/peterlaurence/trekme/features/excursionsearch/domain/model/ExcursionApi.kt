package com.peterlaurence.trekme.features.excursionsearch.domain.model

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionCategory
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionSearchItem
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord

interface ExcursionApi {
    suspend fun search(lat: Double, lon: Double, category: ExcursionCategory?): List<ExcursionSearchItem>
    suspend fun getGeoRecord(id: String, name: String): GeoRecord?
}