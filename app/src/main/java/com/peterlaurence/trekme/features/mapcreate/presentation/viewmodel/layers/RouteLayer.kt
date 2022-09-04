package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.layers

import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.features.mapcreate.domain.interactors.Wgs84ToNormalizedInteractor
import com.peterlaurence.trekme.features.mapcreate.domain.model.NormalizedPos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.makePathDataBuilder
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.*

class RouteLayer(
    private val scope: CoroutineScope,
    private val wgs84ToNormalizedInteractor: Wgs84ToNormalizedInteractor
) {

    fun setGeoRecord(geoRecord: GeoRecord, mapState: MapState) = scope.launch {
        val firstGroup = geoRecord.routeGroups.firstOrNull() ?: return@launch

        val normalized = firstGroup.routes.flatMap {
            it.routeMarkers
        }.asFlow().mapNotNull {
            wgs84ToNormalizedInteractor.getNormalized(it.lat, it.lon)
        }.flowOn(Dispatchers.Default) ?: return@launch

        val routeData = withContext(Dispatchers.Default) {
            makeRouteData(normalized, mapState)
        } ?: return@launch

        val id = UUID.randomUUID().toString()
        mapState.addPath(id, routeData.pathData)
    }

    private suspend fun makeRouteData(
        normalizedPositionsFlow: Flow<NormalizedPos>,
        mapState: MapState
    ): RouteData? {
        val pathBuilder = mapState.makePathDataBuilder()

        normalizedPositionsFlow.collect {
            pathBuilder.addPoint(it.x, it.y)
        }

        return pathBuilder.build()?.let {
            RouteData(it)
        }
    }
}

private data class RouteData(val pathData: PathData)