package com.peterlaurence.trekme.features.map.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.georecord.domain.interactors.IsUriSupportedInteractor
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.map.domain.repository.RouteRepository
import com.peterlaurence.trekme.features.common.domain.interactors.RouteInteractor
import com.peterlaurence.trekme.features.common.domain.interactors.georecord.ImportGeoRecordInteractor
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
    private val routeInteractor: RouteInteractor,
    private val routeRepository: RouteRepository,
    extendedOfferStateOwner: ExtendedOfferStateOwner,
    private val importGeoRecordInteractor: ImportGeoRecordInteractor,
    private val isUriSupportedInteractor: IsUriSupportedInteractor,
    private val app: Application,
    private val appEventBus: AppEventBus,
    private val tracksEventBus: TracksEventBus,
    private val mapFeatureEvents: MapFeatureEvents,
) : ViewModel() {
    private val _tracks = MutableLiveData<List<Route>>()
    val tracks: LiveData<List<Route>> = _tracks

    val hasExtendedOffer: LiveData<Boolean> = extendedOfferStateOwner.purchaseFlow.asLiveData(viewModelScope.coroutineContext).map {
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

    fun removeRoute(route: Route) = viewModelScope.launch {
        /* Immediately set visibility */
        route.visible.value = false

        map?.also { map ->
            routeInteractor.removeRoutesOnMap(map, listOf(route.id))
            _tracks.value = map.routes.value
        }
    }

    /**
     * The business logic of parsing a geo record (given as an [Uri]).
     */
    fun applyUri(uri: Uri) = viewModelScope.launch {
        map?.let {
            importGeoRecordInteractor.applyGeoRecordUriToMap(
                uri,
                app.applicationContext.contentResolver,
                it
            ).let { result ->
                if (result is GeoRecordImportResult.GeoRecordImportOk) {
                    _tracks.postValue(map?.routes?.value ?: listOf())
                }
                /* Notify the rest of the app */
                appEventBus.postGeoRecordImportResult(result)
                tracksEventBus.postTrackImportEvent(result)
            }
        }
    }

    fun isFileSupported(uri: Uri): Boolean {
        return isUriSupportedInteractor.isUriSupported(uri, app.applicationContext.contentResolver)
    }

    fun renameRoute(route: Route, newName: String) {
        route.name.value = newName
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