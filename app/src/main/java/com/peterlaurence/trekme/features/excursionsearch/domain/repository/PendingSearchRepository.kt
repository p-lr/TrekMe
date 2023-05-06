package com.peterlaurence.trekme.features.excursionsearch.domain.repository

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionCategory
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionSearchItem
import com.peterlaurence.trekme.core.location.domain.model.LatLon
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.wmts.domain.model.Point
import com.peterlaurence.trekme.features.excursionsearch.domain.model.ExcursionApi
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * This class isn't thread-safe. Public methods should only be called from the main thread.
 */
@ActivityRetainedScoped
class PendingSearchRepository @Inject constructor(
    private val api: ExcursionApi,
    private val locationSource: LocationSource
) {
    private var query: QueryData? = null

    val locationFlow = MutableStateFlow<LatLon?>(null)

    fun queueSearch(lat: Double, lon: Double, category: ExcursionCategory?) {
        query = QueryAtPlace(lat, lon, category)
    }

    fun queueSearchAtCurrentLocation(category: ExcursionCategory?) {
        query = QueryAtCurrentLocation(category)
    }

    suspend fun search(): Result<List<ExcursionSearchItem>> {
        val query = query ?: return Result.failure(Exception("No query"))

        return runCatching {
            when (query) {
                is QueryAtCurrentLocation -> {
                    val location = locationSource.locationFlow.firstOrNull() ?: return Result.failure(Exception("Could not get location"))
                    locationFlow.value = LatLon(location.latitude, location.longitude)
                    api.search(location.latitude, location.longitude, query.category)
                }
                is QueryAtPlace -> {
                    locationFlow.value = LatLon(query.lat, query.lon)
                    api.search(query.lat, query.lon, query.category)
                }
            }
        }
    }

    private sealed interface QueryData
    private data class QueryAtPlace(val lat: Double, val lon: Double, val category: ExcursionCategory?) : QueryData
    private data class QueryAtCurrentLocation(val category: ExcursionCategory?) : QueryData
}