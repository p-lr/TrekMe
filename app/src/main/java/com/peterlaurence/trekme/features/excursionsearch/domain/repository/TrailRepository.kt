package com.peterlaurence.trekme.features.excursionsearch.domain.repository

import com.peterlaurence.trekme.core.excursion.domain.model.TrailSearchItem
import com.peterlaurence.trekme.core.map.domain.models.BoundingBoxNormalized
import com.peterlaurence.trekme.features.excursionsearch.domain.model.TrailApi
import javax.inject.Inject

class TrailRepository @Inject constructor(
    private val trailApi: TrailApi
) {
    suspend fun search(boundingBox: BoundingBoxNormalized): List<TrailSearchItem> {
        return trailApi.search(boundingBox)
    }
}