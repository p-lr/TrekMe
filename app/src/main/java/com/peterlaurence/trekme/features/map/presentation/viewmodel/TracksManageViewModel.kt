package com.peterlaurence.trekme.features.map.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.georecord.domain.interactors.IsUriSupportedInteractor
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.common.domain.interactors.RemoveRouteInteractor
import com.peterlaurence.trekme.features.common.domain.interactors.ImportGeoRecordInteractor
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.features.map.domain.interactors.RouteInteractor
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.util.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TracksManageViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val routeInteractor: RouteInteractor,
    private val removeRouteInteractor: RemoveRouteInteractor,
    private val mapExcursionInteractor: MapExcursionInteractor,
    extendedOfferStateOwner: ExtendedOfferStateOwner,
    private val isUriSupportedInteractor: IsUriSupportedInteractor,
    private val importGeoRecordInteractor: ImportGeoRecordInteractor,
    private val mapFeatureEvents: MapFeatureEvents,
    private val app: Application
) : ViewModel() {

    val hasExtendedOffer = extendedOfferStateOwner.purchaseFlow.map {
        it == PurchaseState.PURCHASED
    }

    private val _routeImportEvent = Channel<GeoRecordImportResult>(1)
    val routeImportEventFlow = _routeImportEvent.receiveAsFlow()

    private val map: Map?
        get() = mapRepository.getCurrentMap()

    fun getRouteFlow(): StateFlow<List<Route>> {
        return map?.routes ?: MutableStateFlow(emptyList())
    }

    fun getExcursionRefsFlow(): StateFlow<List<ExcursionRef>> {
        return map?.excursionRefs ?: MutableStateFlow(emptyList())
    }

    fun onColorChange(route: Route, color: Long) = viewModelScope.launch {
        val map = map ?: return@launch
        routeInteractor.setRouteColor(map, route, color)
    }

    fun onColorChange(excursion: ExcursionRef, color: Long) = viewModelScope.launch {
        val map = map ?: return@launch
        mapExcursionInteractor.setColor(map, excursion, color)
    }

    fun toggleExcursionVisibility(ref: ExcursionRef) = viewModelScope.launch {
        val map = map ?: return@launch
        mapExcursionInteractor.toggleVisibility(map, ref)
    }

    fun toggleRouteVisibility(route: Route) = viewModelScope.launch {
        val map = map ?: return@launch
        routeInteractor.toggleRouteVisibility(map, route)
    }

    fun onRenameExcursion(ref: ExcursionRef, newName: String) = viewModelScope.launch {
        val map = map ?: return@launch
        mapExcursionInteractor.rename(map, ref, newName)
    }

    fun onRenameRoute(route: Route, newName: String) = viewModelScope.launch {
        val map = map ?: return@launch
        routeInteractor.renameRoute(map, route, newName)
    }

    fun onRemoveExcursion(ref: ExcursionRef) = viewModelScope.launch {
        val map = map ?: return@launch
        mapExcursionInteractor.removeExcursionOnMap(map, ref)
    }

    fun onRemoveRoute(route: Route) = viewModelScope.launch {
        val map = map ?: return@launch
        removeRouteInteractor.removeRoutesOnMap(map, listOf(route.id))
    }

    fun onRouteImport(uri: Uri) = viewModelScope.launch {
        val map = map ?: return@launch

        val isUriValid = isUriSupportedInteractor.isUriSupported(
            uri, app.applicationContext.contentResolver
        )
        if (isUriValid) {
            importGeoRecordInteractor.applyGeoRecordUriToMap(
                uri,
                app.applicationContext.contentResolver,
                map
            ).also { result ->
                _routeImportEvent.send(result)
            }
        }
    }

    fun centerOnExcursion(ref: ExcursionRef) {
        mapFeatureEvents.postGoToExcursion(ref)
    }

    fun centerOnRoute(route: Route) {
        mapFeatureEvents.postGoToRoute(route)
    }
}

