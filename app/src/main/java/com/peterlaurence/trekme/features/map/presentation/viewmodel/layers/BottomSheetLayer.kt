package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.georecord.domain.logic.getGeoStatistics
import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.ui.state.MapState

class BottomSheetLayer(
    private val scope: CoroutineScope,
    private val excursionRepository: ExcursionRepository,
) {
    val state = MutableStateFlow<BottomSheetState>(BottomSheetState.Loading)

    fun setData(route: Route, mapState: MapState, map: Map, excursionData: ExcursionData?) {
        scope.launch {
            state.value = BottomSheetState.Loading

            if (excursionData != null) {
                processExcursion(excursionData)
            } else {
                processStaticRoute(route)
            }
        }
    }

    private suspend fun processExcursion(excursionData: ExcursionData) {
        val excursion = excursionRepository.getExcursion(excursionData.excursionId) ?: return
        val routes = excursionData.routes
        val geoStatistics = withContext(Dispatchers.Default) {
            getGeoStatistics(routes)
        }
        state.value = BottomSheetState.GeoStatisticsAvailable(geoStatistics, excursion.title)
    }

    private suspend fun processStaticRoute(route: Route) {
        val routes = listOf(route)
        val geoStatistics = withContext(Dispatchers.Default) {
            getGeoStatistics(routes)
        }
        state.value = BottomSheetState.GeoStatisticsAvailable(geoStatistics, route.name)
    }

}

sealed interface BottomSheetState {
    data object Loading : BottomSheetState
    data class GeoStatisticsAvailable(
        val stats: GeoStatistics,
        val title: StateFlow<String>
    ): BottomSheetState
}