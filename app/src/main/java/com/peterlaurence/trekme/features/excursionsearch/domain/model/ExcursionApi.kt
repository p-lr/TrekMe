package com.peterlaurence.trekme.features.excursionsearch.domain.model

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionSearchItem
import com.peterlaurence.trekme.features.excursionsearch.presentation.model.ExcursionCategoryChoice

interface ExcursionApi {
    suspend fun search(lat: Double, lon: Double, categoryChoice: ExcursionCategoryChoice): List<ExcursionSearchItem>
}