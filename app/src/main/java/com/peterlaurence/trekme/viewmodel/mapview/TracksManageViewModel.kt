package com.peterlaurence.trekme.viewmodel.mapview

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Route
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import com.peterlaurence.trekme.core.repositories.map.RouteRepository
import com.peterlaurence.trekme.core.repositories.recording.GpxRepository
import com.peterlaurence.trekme.ui.mapview.events.MapViewEventBus
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
    private val trackImporter: TrackImporter,
    private val app: Application,
    private val appEventBus: AppEventBus,
    private val mapViewEventBus: MapViewEventBus,
    private val mapLoader: MapLoader
) : ViewModel() {
    private val _tracks = MutableLiveData<List<Route>>()
    val tracks: LiveData<List<Route>> = _tracks

    val map: Map?
        get() = mapRepository.getCurrentMap()

    init {
        map?.also {
            _tracks.value = it.routes
        }
    }

    fun getRoute(routeId: String): Route? {
        return _tracks.value?.firstOrNull { it.id == routeId }
    }

    fun removeRoute(route: Route) {
        map?.also { map ->
            map.deleteRoute(route)
            viewModelScope.launch {
                routeRepository.deleteRoute(map, route)
            }
            map.routes?.let { routes ->
                _tracks.value = routes
                /* Notify other views */
                mapViewEventBus.postTrackVisibilityChange()
            }
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
                it,
                mapLoader
            ).let { result ->
                if (result is TrackImporter.GpxImportResult.GpxImportOk) {
                    _tracks.postValue(map?.routes ?: listOf())
                }
                /* Notify the rest of the app */
                appEventBus.postGpxImportResult(result)
                mapViewEventBus.postTrackImportEvent(result)
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
        mapViewEventBus.postTrackNameChange()
    }

    fun changeRouteColor(routeId: String, color: String) {
        val route = map?.routes?.firstOrNull { it.id == routeId }
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