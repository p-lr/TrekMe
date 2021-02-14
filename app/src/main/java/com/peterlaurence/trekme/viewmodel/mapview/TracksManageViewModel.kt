package com.peterlaurence.trekme.viewmodel.mapview

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.repositories.map.MapRepository
import com.peterlaurence.trekme.repositories.recording.GpxRepository
import com.peterlaurence.trekme.ui.mapview.events.MapViewEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
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
        private val trackImporter: TrackImporter,
        private val app: Application,
        private val appEventBus: AppEventBus,
        private val mapViewEventBus: MapViewEventBus,
        private val mapLoader: MapLoader
) : ViewModel() {
    private val _tracks = MutableLiveData<List<RouteGson.Route>>()
    val tracks: LiveData<List<RouteGson.Route>> = _tracks

    val map: Map?
        get() = mapRepository.getCurrentMap()

    init {
        map?.also {
            _tracks.value = it.routes
        }
    }

    fun removeRoute(route: RouteGson.Route) {
        map?.also { map ->
            map.routes?.let { routes ->
                routes.remove(route)
                _tracks.value = routes
                saveChanges()
                /* Notify other views */
                mapViewEventBus.postTrackVisibilityChange()
            }
        }
    }

    /**
     * The business logic of parsing a GPX file (given as an [Uri]).
     * It is wrapped in a child [CoroutineScope] because we use an `async` call, which by default
     * defers Exception handling to the calling code. If an unhandled Exception is thrown, it leads
     * to a failure of the parent scope **even if those Exceptions are caught**. We don't want the
     * whole scope of this fragment to fail, hence the child [CoroutineScope].
     *
     * @throws FileNotFoundException
     * @throws TrackImporter.GpxParseException
     */
    suspend fun applyGpxUri(uri: Uri): TrackImporter.GpxImportResult? = coroutineScope {
        map?.let {
            trackImporter.applyGpxUriToMap(uri, app.applicationContext.contentResolver, it, mapLoader).let { result ->
                if (result is TrackImporter.GpxImportResult.GpxImportOk) {
                    _tracks.postValue((_tracks.value ?: listOf()) + result.routes)
                }
                /* Notify the rest of the app */
                appEventBus.postGpxImportResult(result)
                result
            }
        }
    }

    fun isFileSupported(uri: Uri): Boolean {
        return gpxRepository.isFileSupported(uri, app.applicationContext.contentResolver)
    }

    fun renameRoute(route: RouteGson.Route, newName: String) {
        route.name = newName
        saveChanges()

        /* Notify the view */
        mapViewEventBus.postTrackNameChange()
    }

    fun changeRouteColor(routeId: Int, color: String) {
        val route = map?.routes?.firstOrNull { it.id == routeId }
        if (route != null) {
            route.color = color
            saveChanges()
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            map?.also {
                mapLoader.saveRoutes(it)
            }
        }
    }
}