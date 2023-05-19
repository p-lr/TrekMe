package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.layer

import androidx.compose.ui.geometry.Offset
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.map.domain.interactors.Wgs84ToNormalizedInteractor
import com.peterlaurence.trekme.features.map.domain.models.NormalizedPos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.BoundingBox
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.makePathDataBuilder
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.UUID

class RouteLayer(
    private val scope: CoroutineScope,
    private val geoRecordFlow: Flow<GeoRecord>,
    private val mapStateFlow: Flow<MapState>,
    private val wgs84ToNormalizedInteractor: Wgs84ToNormalizedInteractor
) {
    private var lastBoundingBox: BoundingBox? = null

    init {
        scope.launch {
            mapStateFlow.collectLatest { mapState ->
                geoRecordFlow.collectLatest { geoRecord ->
                    setGeoRecord(geoRecord, mapState)
                }
            }
        }
    }

    fun centerOnGeoRecord(mapState: MapState, geoRecordId: UUID) = scope.launch {
        if (geoRecordFlow.first().id != geoRecordId) return@launch
        lastBoundingBox?.also { bb ->
            mapState.scrollTo(bb, Offset(0.2f, 0.2f))
        }
    }

    private suspend fun setGeoRecord(geoRecord: GeoRecord, mapState: MapState) {
        val firstGroup = geoRecord.routeGroups.firstOrNull() ?: return

        val normalized = firstGroup.routes.flatMap {
            it.routeMarkers
        }.asFlow().mapNotNull {
            wgs84ToNormalizedInteractor.getNormalized(it.lat, it.lon)
        }.flowOn(Dispatchers.Default)

        val routeData = withContext(Dispatchers.Default) {
            makeRouteData(normalized, mapState)
        } ?: return

        val id = UUID.randomUUID().toString()
        mapState.addPath(id, routeData.pathData)

        mapState.scrollTo(routeData.boundingBox, Offset(0.2f, 0.2f))
        lastBoundingBox = routeData.boundingBox
    }

    private suspend fun makeRouteData(
        normalizedPositionsFlow: Flow<NormalizedPos>,
        mapState: MapState
    ): RouteData? {
        val pathBuilder = mapState.makePathDataBuilder()

        var xMin: Double? = null
        var xMax: Double? = null
        var yMin: Double? = null
        var yMax: Double? = null

        normalizedPositionsFlow.collect {
            pathBuilder.addPoint(it.x, it.y)

            xMin = it.x.coerceAtMost(xMin ?: it.x)
            xMax = it.x.coerceAtLeast(xMax ?: it.x)
            yMin = it.y.coerceAtMost(yMin ?: it.y)
            yMax = it.y.coerceAtLeast(yMax ?: it.y)
        }

        val boundingBox = if (xMin != null && xMax != null && yMin != null && yMax != null) {
            BoundingBox(xLeft = xMin!!, xRight = xMax!!, yBottom = yMax!!, yTop = yMin!!)
        } else return null

        return pathBuilder.build()?.let {
            RouteData(it, boundingBox)
        }
    }
}

private data class RouteData(val pathData: PathData, val boundingBox: BoundingBox)