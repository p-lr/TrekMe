package com.peterlaurence.trekme.core.repositories.mapcreate

import com.peterlaurence.trekme.core.lib.geocoding.GeoPlace
import com.peterlaurence.trekme.core.lib.geocoding.GeocodingEngine
import com.peterlaurence.trekme.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocodingRepository @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope
) {
    private val geocodingEngine = GeocodingEngine(scope)

    val geoPlaceFlow: Flow<List<GeoPlace>?> = geocodingEngine.geoPlaceFlow

    fun search(placeName: String) {
        geocodingEngine.search(placeName)
    }
}