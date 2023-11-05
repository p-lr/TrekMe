package com.peterlaurence.trekme.features.excursionsearch.domain.model

import com.peterlaurence.trekme.core.excursion.domain.model.TrailDetail
import com.peterlaurence.trekme.core.excursion.domain.model.TrailSearchItem
import com.peterlaurence.trekme.core.map.domain.models.BoundingBoxNormalized


interface TrailApi {
    suspend fun search(boundingBox: BoundingBoxNormalized): List<TrailSearchItem>

    suspend fun getDetails(boundingBox: BoundingBoxNormalized, ids: List<String>): List<TrailDetail>
}