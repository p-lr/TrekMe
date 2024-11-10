package com.peterlaurence.trekme.core.geocoding.domain.engine

import com.peterlaurence.trekme.core.geocoding.domain.model.GeocodingBackend
import com.peterlaurence.trekme.core.geocoding.di.NominatimBackend
import com.peterlaurence.trekme.core.geocoding.di.PhotonBackend
import com.peterlaurence.trekme.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A wrapper around several geocoding APIs. A place name is submitted as a raw [String], using the
 * public [search] method. A geocoding API resolves a query name to list of actual geo-localized
 * places ([GeoPlace]).
 *
 * As a geocoding API might not always be available or may just return empty result, this wrapper
 * leverages several geocoding APIs and use them in a serial fashion. By convention, an API shall
 * return null in case of error or empty result.
 *
 * Clients to resolved [GeoPlace]s should collect the public [geoPlaceFlow].
 *
 * @author P.Laurence on 01/01/21
 */
class GeocodingEngine @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    @PhotonBackend private val photon: GeocodingBackend,
    @NominatimBackend private val nominatim: GeocodingBackend
) {
    private val backends: List<GeocodingBackend> = listOf(nominatim, photon)

    private val _isLoadingState = MutableStateFlow(false)
    val isLoadingState: StateFlow<Boolean> = _isLoadingState.asStateFlow()

    private val _geoPlaceFlow = MutableSharedFlow<List<GeoPlace>>(0, 1, BufferOverflow.DROP_OLDEST)
    val geoPlaceFlow: SharedFlow<List<GeoPlace>> = _geoPlaceFlow.asSharedFlow()

    private val queryFlow = MutableSharedFlow<String>(0, 1, BufferOverflow.DROP_OLDEST)
    private val queryFlowDebounced = queryFlow.debounce(500)

    init {
        collectQueries()
    }

    fun search(query: String) {
        queryFlow.tryEmit(query)
    }

    private fun collectQueries() = scope.launch {
        queryFlowDebounced.collect { query ->
            _isLoadingState.update { true }
            val geoPlaces = runCatching {
                for (backend in backends) {
                    val geoPlaces = backend.search(query)
                    if (!geoPlaces.isNullOrEmpty()) {
                        return@runCatching geoPlaces
                    }
                }
                emptyList()
            }.getOrNull()
            _isLoadingState.update { false }

            if (geoPlaces != null) {
                _geoPlaceFlow.tryEmit(geoPlaces)
            }
        }
    }
}

data class GeoPlace(val type: GeoType, val name: String, val locality: String, val lat: Double,
                    val lon: Double)

sealed class GeoType
object POI : GeoType()
object City : GeoType()
object Street : GeoType()