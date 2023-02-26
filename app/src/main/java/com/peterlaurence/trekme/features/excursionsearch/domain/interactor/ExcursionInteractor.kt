package com.peterlaurence.trekme.features.excursionsearch.domain.interactor

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionSearchItem
import com.peterlaurence.trekme.features.excursionsearch.domain.model.ExcursionApi
import com.peterlaurence.trekme.features.excursionsearch.presentation.model.ExcursionCategoryChoice
import javax.inject.Inject

class ExcursionInteractor @Inject constructor(
    private val api: ExcursionApi
) {
    suspend fun search(lat: Double, lon: Double, categoryChoice: ExcursionCategoryChoice): List<ExcursionSearchItem> {
        return api.search(lat, lon, categoryChoice)
    }
}