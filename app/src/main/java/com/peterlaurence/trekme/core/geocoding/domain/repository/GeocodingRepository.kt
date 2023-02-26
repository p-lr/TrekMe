package com.peterlaurence.trekme.core.geocoding.domain.repository

import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeocodingEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocodingRepository @Inject constructor(
    private val geocodingEngine: GeocodingEngine,
) {
    val geoPlaceFlow: Flow<List<GeoPlace>> = geocodingEngine.geoPlaceFlow
    val isLoadingFlow: StateFlow<Boolean> = geocodingEngine.isLoadingState

    fun search(placeName: String) {
        geocodingEngine.search(placeName)
    }
}