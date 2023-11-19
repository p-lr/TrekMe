package com.peterlaurence.trekme.features.excursionsearch.domain.model

import com.peterlaurence.trekme.core.excursion.domain.model.TrailDetail
import com.peterlaurence.trekme.core.excursion.domain.model.TrailDetailWithElevation
import com.peterlaurence.trekme.core.excursion.domain.model.TrailSearchItem
import com.peterlaurence.trekme.core.map.domain.models.BoundingBoxNormalized


interface TrailApi {
    suspend fun search(boundingBox: BoundingBoxNormalized): List<TrailSearchItem>

    /**
     * Get the list of [TrailDetail] given a [boundingBox] and a list of relation [ids].
     * The returned objects doesn't have elevation data.
     */
    suspend fun getDetails(boundingBox: BoundingBoxNormalized, ids: List<String>): List<TrailDetail>

    /**
     * Given an [id], get the full detail of a relation, with elevation data.
     */
    suspend fun getDetailsWithElevation(id: String): TrailDetailWithElevation?
}