package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.geotools.distanceApprox
import com.peterlaurence.trekme.core.map.domain.models.Landmark
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.map.domain.core.getLonLatFromNormalizedCoordinate
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.components.LandMark
import com.peterlaurence.trekme.features.map.presentation.ui.components.LandmarkCallout
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapViewModel
import com.peterlaurence.trekme.features.map.presentation.viewmodel.controllers.positionCallout
import com.peterlaurence.trekme.util.dpToPx
import com.peterlaurence.trekme.util.throttle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

class LandmarkLayer(
    private val scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val mapInteractor: MapInteractor
) : MapViewModel.MarkerTapListener {
    private var landmarkListState = mutableMapOf<String, LandmarkState>()

    init {
        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                onMapUpdate(map, mapState)
            }
        }
    }

    private suspend fun onMapUpdate(map: Map, mapState: MapState) {
        mapInteractor.getLandmarksFlow(map).collect {
            for (landmarkWithNormalizedPos in it) {
                val existing = landmarkListState[landmarkWithNormalizedPos.landmark.id]
                if (existing != null) {
                    existing.apply {
                        landmark = landmarkWithNormalizedPos.landmark
                        mapState.moveMarker(
                            existing.idOnMap,
                            landmarkWithNormalizedPos.x,
                            landmarkWithNormalizedPos.y
                        )
                    }
                } else {
                    val landmarkState = addLandmarkOnMap(
                        landmarkWithNormalizedPos.landmark,
                        mapState,
                        landmarkWithNormalizedPos.x,
                        landmarkWithNormalizedPos.y
                    )
                    landmarkListState[landmarkWithNormalizedPos.landmark.id] = landmarkState
                }
            }
            val iter = landmarkListState.iterator()
            val ids = it.map { b -> b.landmark.id }
            for (entry in iter) {
                if (entry.key !in ids && entry.value.isStatic) iter.remove()
            }
        }
    }

    fun addLandmark() = scope.launch {
        val (map, mapState) = dataStateFlow.first()

        val x = mapState.centroidX
        val y = mapState.centroidY
        val landmark = mapInteractor.makeLandmark(map, x, y)
        val landmarkState = addLandmarkOnMap(landmark, mapState, x, y)
        morphToDynamic(landmarkState, x, y, mapState)
        landmarkListState[landmark.id] = landmarkState
    }

    override fun onMarkerTap(
        mapState: MapState,
        mapId: UUID,
        id: String,
        x: Double,
        y: Double
    ): Boolean {
        if (id.startsWith(markerGrabPrefix)) {
            onMarkerGrabTap(id, mapState)
            return true
        }
        val landmarkId = id.substringAfter("$landmarkPrefix-")
        val landmarkState = landmarkListState[landmarkId] ?: return false

        scope.launch {
            var shouldAnimate by mutableStateOf(true)

            val calloutHeight = dpToPx(landmarkCalloutHeightDp).toInt()
            val markerHeight =
                dpToPx(48f).toInt() // The view height is 48dp, but only the top half is used to draw the marker.
            val calloutWidth = dpToPx(landmarkCalloutWidthDp).toInt()
            val markerWidth = dpToPx(24f).toInt()

            val pos = positionCallout(
                mapState,
                calloutWidth,
                calloutHeight,
                x,
                y,
                markerWidth,
                markerHeight
            )

            val calloutId = "$calloutPrefix-$landmarkId"
            mapState.addCallout(
                calloutId, x, y,
                relativeOffset = Offset(pos.relativeAnchorLeft, pos.relativeAnchorTop),
                absoluteOffset = Offset(pos.absoluteAnchorLeft, pos.absoluteAnchorTop),
                autoDismiss = true, clickable = false, zIndex = 3f
            ) {
                val subTitle = landmarkListState[landmarkId]?.landmark?.let {
                    "${stringResource(id = R.string.latitude_short)} : ${df.format(it.lat)}  " +
                            "${stringResource(id = R.string.longitude_short)} : ${df.format(it.lon)}"
                } ?: ""

                LandmarkCallout(
                    DpSize(landmarkCalloutWidthDp.dp, landmarkCalloutHeightDp.dp),
                    subTitle = subTitle,
                    shouldAnimate,
                    onAnimationDone = { shouldAnimate = false },
                    onDeleteAction = {
                        mapState.removeCallout(calloutId)
                        mapState.removeMarker(landmarkState.idOnMap)
                        mapInteractor.deleteLandmark(landmarkState.landmark, mapId)
                    },
                    onMoveAction = {
                        mapState.removeCallout(calloutId)
                        morphToDynamic(landmarkState, x, y, mapState)
                    }
                )
            }
        }
        return true
    }

    private fun attachMarkerGrab(
        markerId: String,
        xMarker: Double,
        yMarker: Double,
        mapState: MapState,
        landmarkState: LandmarkState
    ) {
        val grabId = "$markerGrabPrefix-$markerId"
        mapState.addMarker(grabId, xMarker, yMarker, Offset(-0.5f, -0.5f), zIndex = 0f) {
            MarkerGrab(
                morphedIn = !landmarkState.isStatic,
                onMorphOutDone = {
                    mapState.removeMarker(grabId)
                }
            )
        }
        mapState.enableMarkerDrag(grabId) { _, x, y, dx, dy, _, _ ->
            mapState.moveMarker(grabId, x + dx, y + dy)
            mapState.moveMarker(markerId, x + dx, y + dy)
        }
    }

    private fun onMarkerGrabTap(markerGrabId: String, mapState: MapState) {
        val markerId = markerGrabId.substringAfter("$landmarkPrefix-")
        val landmarkState = landmarkListState[markerId] ?: return
        landmarkState.isStatic = true

        mapState.updateMarkerClickable(landmarkState.idOnMap, true)

        val landmarkInfo = mapState.getMarkerInfo(landmarkState.idOnMap) ?: return
        val landmark = landmarkState.landmark
        scope.launch {
            dataStateFlow.first().also {
                mapInteractor.updateAndSaveLandmark(
                    landmark,
                    it.map,
                    landmarkInfo.x,
                    landmarkInfo.y
                )
            }
        }
    }

    private fun addLandmarkOnMap(
        landmark: Landmark, mapState: MapState, x: Double, y: Double
    ): LandmarkState {
        val id = "$landmarkPrefix-${landmark.id}"
        val state = LandmarkState(id, landmark)

        mapState.addMarker(
            id,
            x,
            y,
            relativeOffset = Offset(-0.5f, -0.5f),
            zIndex = 1f,
            clickableAreaCenterOffset = Offset(0f, -0.22f),
            clickableAreaScale = Offset(0.7f, 0.5f)
        ) {
            LandMark(Modifier.padding(5.dp), state.isStatic)
        }
        return state
    }

    private fun morphToDynamic(
        landmarkState: LandmarkState, x: Double, y: Double, mapState: MapState
    ) {
        mapState.updateMarkerClickable(landmarkState.idOnMap, false)
        landmarkState.isStatic = false
        attachMarkerGrab(landmarkState.idOnMap, x, y, mapState, landmarkState)
    }
}

class LandmarkLinesState(mapState: MapState, private val map: Map) {
    private val markersSnapshot by mapState.markerDerivedState()
    private val markersSnapshotFlow = mapState.markerSnapshotFlow()

    val positionMarkerSnapshot: MarkerDataSnapshot?
        get() = markersSnapshot.firstOrNull {
            it.id == positionMarkerId
        }

    val landmarksSnapshot: List<MarkerDataSnapshot>
        get() = markersSnapshot.filter {
            it.id.startsWith(landmarkPrefix)
        }

    val distanceForLandmark: Flow<kotlin.collections.Map<String, Double?>> =
        markersSnapshotFlow.throttle(100).mapNotNull {
            val position = positionMarkerSnapshot ?: return@mapNotNull null
            computeDistanceForId(landmarksSnapshot, position)
        }


    private suspend fun computeDistanceForId(
        landmarks: List<MarkerDataSnapshot>,
        position: MarkerDataSnapshot
    ): kotlin.collections.Map<String, Double?> = withContext(Dispatchers.Default) {
        suspend fun computeDistance(aX: Double, aY: Double, bX: Double, bY: Double): Double {
            val lonLatA = getLonLatFromNormalizedCoordinate(aX, aY, map.projection, map.mapBounds)
            val lonLatB = getLonLatFromNormalizedCoordinate(bX, bY, map.projection, map.mapBounds)
            return distanceApprox(lonLatA[1], lonLatA[0], lonLatB[1], lonLatB[0])
        }

        landmarks.associate {
            it.id to computeDistance(position.x, position.y, it.x, it.y)
        }
    }
}

private const val landmarkPrefix = "landmark"
private const val calloutPrefix = "callout"
private const val markerGrabPrefix = "grabLandmark"

private class LandmarkState(val idOnMap: String, initLandmark: Landmark) {
    var landmark by mutableStateOf(initLandmark)
    var isStatic by mutableStateOf(true)
}

private val df = DecimalFormat("#.####").apply {
    roundingMode = RoundingMode.CEILING
}

private const val landmarkCalloutWidthDp = 140
private const val landmarkCalloutHeightDp = 100
