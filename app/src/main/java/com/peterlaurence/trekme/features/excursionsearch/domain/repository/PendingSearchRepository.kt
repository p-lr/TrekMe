package com.peterlaurence.trekme.features.excursionsearch.domain.repository

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionCategory
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionSearchItem
import com.peterlaurence.trekme.core.location.domain.model.LatLon
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.features.excursionsearch.domain.model.ExcursionApi
import com.peterlaurence.trekme.util.ResultL
import com.peterlaurence.trekme.util.asResultL
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject


@ActivityRetainedScoped
class PendingSearchRepository @Inject constructor(
    private val api: ExcursionApi,
    private val locationSource: LocationSource
) {
    /**
     * Using a [MutableStateFlow] so that when a new identical search (respective to equals method)
     * is queued
     */
    private var queryFlow = MutableStateFlow<QueryData?>(null)

    val locationFlow = MutableStateFlow<LatLon?>(null)

    fun queueSearch(lat: Double, lon: Double, category: ExcursionCategory?) {
        queryFlow.value = QueryAtPlace(lat, lon, category)
    }

    fun queueSearchAtCurrentLocation(category: ExcursionCategory?) {
        queryFlow.value = QueryAtCurrentLocation(category)
    }

    fun getSearchResultFlow(): Flow<ResultL<List<ExcursionSearchItem>>> = channelFlow {
        queryFlow.collect { query ->
            send(ResultL.loading())
            send(
                if (query != null) {
                    runCatching {
                        when (query) {
                            is QueryAtCurrentLocation -> {
                                val location =
                                    locationSource.locationFlow.firstOrNull() ?: throw Exception("Could not get location")
                                locationFlow.value =
                                    LatLon(location.latitude, location.longitude)
                                api.search(
                                    location.latitude,
                                    location.longitude,
                                    query.category
                                )
                            }

                            is QueryAtPlace -> {
                                locationFlow.value = LatLon(query.lat, query.lon)
                                api.search(query.lat, query.lon, query.category)
                            }
                        }
                    }.asResultL()
                } else {
                    ResultL.loading()
                }
            )
        }
    }

    private sealed interface QueryData
    private data class QueryAtPlace(val lat: Double, val lon: Double, val category: ExcursionCategory?) : QueryData
    private data class QueryAtCurrentLocation(val category: ExcursionCategory?) : QueryData
}