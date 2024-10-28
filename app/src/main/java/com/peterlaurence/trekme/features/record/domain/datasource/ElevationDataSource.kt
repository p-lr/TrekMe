package com.peterlaurence.trekme.features.record.domain.datasource

import com.peterlaurence.trekme.features.record.domain.datasource.model.ApiStatus
import com.peterlaurence.trekme.features.record.domain.datasource.model.ElevationResult

interface ElevationDataSource {
    suspend fun getElevations(latList: List<Double>, lonList: List<Double>): ElevationResult
    suspend fun checkStatus(): ApiStatus
    fun isInCoverage(lat: Double, lon: Double): Boolean
}