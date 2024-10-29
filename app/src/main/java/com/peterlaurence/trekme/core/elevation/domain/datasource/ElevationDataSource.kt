package com.peterlaurence.trekme.core.elevation.domain.datasource

import com.peterlaurence.trekme.core.elevation.domain.model.ElevationResult

interface ElevationDataSource {
    suspend fun getElevations(latList: List<Double>, lonList: List<Double>): ElevationResult
    suspend fun checkStatus(): Boolean
    fun isInCoverage(lat: Double, lon: Double): Boolean
}