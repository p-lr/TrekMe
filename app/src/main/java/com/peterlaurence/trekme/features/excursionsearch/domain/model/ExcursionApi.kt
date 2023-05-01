package com.peterlaurence.trekme.features.excursionsearch.domain.model

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionCategory
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionSearchItem

interface ExcursionApi {
    suspend fun search(lat: Double, lon: Double, category: ExcursionCategory?): List<ExcursionSearchItem>
}