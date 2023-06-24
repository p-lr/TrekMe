package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.layer

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.location.domain.model.LatLon
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.domain.interactors.Wgs84ToMercatorInteractor
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.contains
import com.peterlaurence.trekme.features.excursionsearch.presentation.ui.component.Cursor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.hasCallout
import ovh.plrapps.mapcompose.api.makePathDataBuilder
import ovh.plrapps.mapcompose.api.moveCallout
import ovh.plrapps.mapcompose.api.removeAllPaths
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.UUID
import ovh.plrapps.mapcompose.api.BoundingBox as NormalizedBoundingBox

class RouteLayer(
    private val scope: CoroutineScope,
    private val geoRecordFlow: Flow<GeoRecord>,
    private val mapStateFlow: Flow<MapState>,
    private val wgs84ToMercatorInteractor: Wgs84ToMercatorInteractor,
    private val getMapInteractor: GetMapInteractor
) {
    private val boundingBoxData: MutableStateFlow<BoundingBoxData?> = MutableStateFlow(null)
    val hasContainingMap: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val cursorChannel = Channel<CursorData>(Channel.CONFLATED)
    private val cursorMarkerId = "cursor"
    private var distance by mutableStateOf(0.0)
    private var elevation by mutableStateOf(0.0)

    init {
        scope.launch {
            mapStateFlow.collectLatest { mapState ->
                boundingBoxData.value = null
                geoRecordFlow.collectLatest { geoRecord ->
                    setGeoRecord(geoRecord, mapState)
                }
            }
        }

        scope.launch {
            mapStateFlow.collectLatest { mapState ->
                for (data in cursorChannel) {
                    distance = data.distance
                    elevation = data.ele
                    val normalized = withContext(Dispatchers.Default) {
                        wgs84ToMercatorInteractor.getNormalized(data.latLon.lat, data.latLon.lon)
                    } ?: continue
                    if (mapState.hasCallout(cursorMarkerId)) {
                        mapState.moveCallout(cursorMarkerId, normalized.x, normalized.y)
                    } else {
                        mapState.addCallout(
                            id = cursorMarkerId,
                            x = normalized.x,
                            y = normalized.y,
                            relativeOffset = Offset(-0.5f, -1f),
                            zIndex = 1f
                        ) {
                            Cursor(
                                modifier = Modifier.padding(bottom = 18.dp),
                                distance = distance,
                                elevation = elevation
                            )
                        }
                    }
                }
            }
        }

        scope.launch {
            boundingBoxData.collect { bb ->
                if (bb != null) {
                    hasContainingMap.value = hasContainingMap(bb.denormalized)
                }
            }
        }
    }

    /**
     * If the user has a map which contains the bounding box of the selected excursion, then
     * by default we de-select the option to download the corresponding map (since upon excursion
     * download, we import it into all maps which can display the excursion).
     */
    private suspend fun hasContainingMap(boundingBox: BoundingBox): Boolean {
        var hasMapContainingBoundingBox = false

        coroutineScope {
            launch {
                val scope = this
                getMapInteractor.getMapList().map { map ->
                    launch {
                        if (map.contains(boundingBox)) {
                            hasMapContainingBoundingBox = true
                            scope.cancel()
                        }
                    }
                }
            }
        }

        return hasMapContainingBoundingBox
    }

    fun centerOnGeoRecord(mapState: MapState, geoRecordId: UUID) = scope.launch {
        if (geoRecordFlow.first().id != geoRecordId) return@launch
        boundingBoxData.value?.normalized?.also { bb ->
            mapState.scrollTo(bb, Offset(0.2f, 0.2f))
        }
    }

    fun setCursor(latLon: LatLon, distance: Double, ele: Double) {
        cursorChannel.trySend(CursorData(latLon, distance, ele))
    }

    private suspend fun setGeoRecord(geoRecord: GeoRecord, mapState: MapState) {
        val firstGroup = geoRecord.routeGroups.firstOrNull() ?: return

        val markersFlow = firstGroup.routes.flatMap {
            it.routeMarkers
        }.asFlow()

        val routeData = withContext(Dispatchers.Default) {
            makeRouteData(markersFlow, mapState)
        } ?: return

        boundingBoxData.value = routeData.boundingBoxData

        /* Remove all paths before adding the new one */
        mapState.removeAllPaths()
        val id = UUID.randomUUID().toString()
        mapState.addPath(id, routeData.pathData)

        mapState.scrollTo(routeData.boundingBoxData.normalized, Offset(0.2f, 0.2f))
    }

    private suspend fun makeRouteData(
        markersFlow: Flow<Marker>,
        mapState: MapState
    ): RouteData? {
        val pathBuilder = mapState.makePathDataBuilder()

        var latMin: Double? = null
        var latMax: Double? = null
        var lonMin: Double? = null
        var lonMax: Double? = null

        markersFlow.collect {
            val normalized = wgs84ToMercatorInteractor.getNormalized(it.lat, it.lon)
            if (normalized != null) {
                pathBuilder.addPoint(normalized.x, normalized.y)
            }

            latMin = it.lat.coerceAtMost(latMin ?: it.lat)
            latMax = it.lat.coerceAtLeast(latMax ?: it.lat)
            lonMin = it.lon.coerceAtMost(lonMin ?: it.lon)
            lonMax = it.lon.coerceAtLeast(lonMax ?: it.lon)
        }

        val minLat = latMin
        val minLon = lonMin
        val maxLat = latMax
        val maxLon = lonMax

        val normalizedBoundingBox =
            if (minLat != null && minLon != null && maxLat != null && maxLon != null) {
                val bottomLeft =
                    wgs84ToMercatorInteractor.getNormalized(minLat, minLon) ?: return null
                val topRight =
                    wgs84ToMercatorInteractor.getNormalized(maxLat, maxLon) ?: return null
                NormalizedBoundingBox(bottomLeft.x, topRight.y, topRight.x, bottomLeft.y)
            } else return null

        val denormalizedBoundingBox = BoundingBox(minLat, maxLat, minLon, maxLon)

        val bbData = BoundingBoxData(
            normalized = normalizedBoundingBox,
            denormalized = denormalizedBoundingBox
        )

        return pathBuilder.build()?.let {
            RouteData(it, bbData)
        }
    }
}

private data class RouteData(val pathData: PathData, val boundingBoxData: BoundingBoxData)
private data class BoundingBoxData(
    val normalized: NormalizedBoundingBox,
    val denormalized: BoundingBox
)

private data class CursorData(val latLon: LatLon, val distance: Double, val ele: Double)