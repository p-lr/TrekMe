package com.peterlaurence.trekme.features.map.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.peterlaurence.trekme.billing.common.PurchaseState
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import com.peterlaurence.trekme.core.repositories.map.RouteRepository
import com.peterlaurence.trekme.core.repositories.offers.extended.ExtendedOfferRepository
import com.peterlaurence.trekme.core.repositories.recording.GpxRepository
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.map.presentation.ui.legacy.events.TracksEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The view-model of the list of tracks, accessible when viewing a map.
 *
 * @author P.Laurence on 25/09/2020
 */
@HiltViewModel
class TracksManageViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val gpxRepository: GpxRepository,
    private val routeRepository: RouteRepository,
    extendedOfferRepository: ExtendedOfferRepository,
    private val trackImporter: TrackImporter,
    private val app: Application,
    private val appEventBus: AppEventBus,
    private val tracksEventBus: TracksEventBus,
    private val mapFeatureEvents: MapFeatureEvents,
) : ViewModel() {
    private val _tracks = MutableLiveData<List<Route>>()
    val tracks: LiveData<List<Route>> = _tracks

    val hasExtendedOffer: LiveData<Boolean> = extendedOfferRepository.purchaseFlow.asLiveData(viewModelScope.coroutineContext).map {
            it == PurchaseState.PURCHASED
        }

    val map: Map?
        get() = mapRepository.getCurrentMap()

    init {
        map?.also {
            _tracks.value = it.routes.value
        }
    }

    fun getRoute(routeId: String): Route? {
        return _tracks.value?.firstOrNull { it.id == routeId }
    }

    fun removeRoute(route: Route) {
        /* Immediately set visibility */
        route.visible.value = false

        map?.also { map ->
            map.deleteRoute(route)
            viewModelScope.launch {
                routeRepository.deleteRoute(map, route)
            }
            _tracks.value = map.routes.value
        }
    }

    /**
     * The business logic of parsing a GPX file (given as an [Uri]).
     */
    fun applyGpxUri(uri: Uri) = viewModelScope.launch {
        map?.let {
            trackImporter.applyGpxUriToMap(
                uri,
                app.applicationContext.contentResolver,
                it
            ).let { result ->
                if (result is TrackImporter.GpxImportResult.GpxImportOk) {
                    _tracks.postValue(map?.routes?.value ?: listOf())
                }
                /* Notify the rest of the app */
                appEventBus.postGpxImportResult(result)
                tracksEventBus.postTrackImportEvent(result)
            }
        }
    }

    fun isFileSupported(uri: Uri): Boolean {
        return gpxRepository.isFileSupported(uri, app.applicationContext.contentResolver)
    }

    fun renameRoute(route: Route, newName: String) {
        route.name = newName
        saveChanges(route)

        /* Notify the view */
        tracksEventBus.postTrackNameChange()
    }

    fun goToRouteOnMap(route: Route) {
        mapFeatureEvents.postGoToRoute(route)
    }

    fun changeRouteColor(routeId: String, color: String) {
        val route = map?.routes?.value?.firstOrNull { it.id == routeId }
        if (route != null) {
            route.color.value = color
            saveChanges(route)
        }
    }

    fun saveChanges(route: Route) {
        viewModelScope.launch {
            map?.also {
                routeRepository.saveRouteInfo(it, route)
            }
        }
    }
}