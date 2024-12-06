package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.georecord.domain.logic.getGeoStatistics
import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import com.peterlaurence.trekme.features.common.domain.interactors.RemoveRouteInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.RouteInteractor
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.features.trailsearch.presentation.ui.component.ElevationGraphPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setVisibleAreaPadding
import ovh.plrapps.mapcompose.api.BoundingBox as NormalizedBoundingBox

class BottomSheetLayer(
    private val scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val excursionRepository: ExcursionRepository,
    private val mapInteractor: MapInteractor,
    private val mapExcursionInteractor: MapExcursionInteractor,
    private val routeInteractor: RouteInteractor,
    private val removeRouteInteractor: RemoveRouteInteractor
) {
    val state = MutableStateFlow<BottomSheetState>(BottomSheetState.Loading)

    fun setData(route: Route, excursionData: ExcursionData?) {
        val id = excursionData?.excursionRef?.id ?: route.id
        if (state.value.let { it is BottomSheetState.BottomSheetData && it.type.id == id }) return

        scope.launch {
            state.value = BottomSheetState.Loading

            if (excursionData != null) {
                processExcursion(excursionData)
            } else {
                processStaticRoute(route)
            }
        }
    }

    fun onBottomPadding(bottom: Dp, withCenter: Boolean = false) {
        scope.launch {
            val (map, mapState) = dataStateFlow.firstOrNull() ?: return@launch
            mapState.setVisibleAreaPadding(bottom = bottom)

            val currentState = state.value
            if (withCenter && currentState is BottomSheetState.BottomSheetData) {
                val bb = currentState.stats.boundingBox ?: return@launch
                val pos1 = mapInteractor.getNormalizedCoordinates(
                    map = map,
                    latitude = bb.minLat,
                    longitude = bb.minLon
                )
                val pos2 = mapInteractor.getNormalizedCoordinates(
                    map = map,
                    latitude = bb.maxLat,
                    longitude = bb.maxLon
                )

                mapState.scrollTo(
                    area = NormalizedBoundingBox(
                        xLeft = pos1.x,
                        yTop = pos2.y,
                        xRight = pos2.x,
                        yBottom = pos1.y
                    ),
                    padding = Offset(0.1f, 0.1f)
                )
            }
        }
    }

    fun onColorChange(color: Long, type: TrackType) = scope.launch {
        val (map, _) = dataStateFlow.firstOrNull() ?: return@launch
        when (type) {
            is TrackType.ExcursionType -> {
                mapExcursionInteractor.setColor(map, type.excursionRef, color)
            }

            is TrackType.RouteType -> {
                routeInteractor.setRouteColor(map, type.route, color)
            }
        }
    }

    fun onTitleChange(title: String, type: TrackType) = scope.launch {
        when (type) {
            is TrackType.ExcursionType -> {
                mapExcursionInteractor.rename(type.excursionRef, title)
            }

            is TrackType.RouteType -> {
                val (map, _) = dataStateFlow.firstOrNull() ?: return@launch
                routeInteractor.renameRoute(map, type.route, title)
            }
        }
    }

    fun onRemoveExcursion(ref: ExcursionRef) = scope.launch {
        val (map, _) = dataStateFlow.firstOrNull() ?: return@launch
        mapExcursionInteractor.removeExcursionOnMap(map, ref)
    }

    fun onRemoveRoute(route: Route) = scope.launch {
        val (map, _) = dataStateFlow.firstOrNull() ?: return@launch
        removeRouteInteractor.removeRoutesOnMap(map, listOf(route.id))
    }

    private suspend fun processExcursion(excursionData: ExcursionData) {
        val excursion = excursionRepository.getExcursion(excursionData.excursionRef.id) ?: return
        val routes = excursionData.routes

        processTrack(
            routes = routes,
            type = TrackType.ExcursionType(excursionData.excursionRef, excursion.isPathEditable),
            title = excursion.title,
            color = excursionData.excursionRef.color
        )
    }

    private suspend fun processStaticRoute(route: Route) {
        val routes = listOf(route)
        processTrack(routes, TrackType.RouteType(route), route.name, route.color)
    }

    private suspend fun processTrack(
        routes: List<Route>,
        type: TrackType,
        title: StateFlow<String>,
        color: StateFlow<String>
    ) {
        val points = mutableListOf<ElevationGraphPoint>()
        val geoStatistics = withContext(Dispatchers.Default) {
            getGeoStatistics(routes) { distance, marker ->
                marker.elevation?.also { ele ->
                    points += ElevationGraphPoint(marker.lat, marker.lon, distance, ele)
                }
            }
        }
        val elevationGraphPoints = points.ifEmpty { null }

        state.value = BottomSheetState.BottomSheetData(
            type = type,
            title = title,
            color = color,
            stats = geoStatistics,
            elevationGraphPoints = elevationGraphPoints
        )
    }
}

sealed interface BottomSheetState {
    data object Loading : BottomSheetState
    data class BottomSheetData(
        val type: TrackType,
        val title: StateFlow<String>,
        val color: StateFlow<String>,
        val stats: GeoStatistics,
        val elevationGraphPoints: List<ElevationGraphPoint>?,
    ) : BottomSheetState
}

val BottomSheetState.BottomSheetData.hasElevation: Boolean
    get() = elevationGraphPoints != null && elevationGraphPoints.size > 1

sealed interface TrackType {
    data class RouteType(val route: Route) : TrackType
    data class ExcursionType(val excursionRef: ExcursionRef, val isPathEditable: Boolean) : TrackType
}

private val TrackType.id: String
    get() = when (this) {
        is TrackType.ExcursionType -> excursionRef.id
        is TrackType.RouteType -> route.id
    }