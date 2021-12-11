package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.geotools.distanceApprox
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Landmark
import com.peterlaurence.trekme.core.map.getLonLat
import com.peterlaurence.trekme.features.map.presentation.ui.components.LandMark
import com.peterlaurence.trekme.features.map.presentation.ui.components.LandmarkCallout
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.viewmodel.LayerData
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapUiState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapViewModel
import com.peterlaurence.trekme.features.map.presentation.viewmodel.controllers.positionCallout
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

class LandmarkLayer(
    private val scope: CoroutineScope,
    private val layerData: Flow<LayerData>,
    private val mapInteractor: MapInteractor
) : MapViewModel.MarkerTapListener {
    private var landmarkListState = mapOf<String, LandmarkState>()

    init {
        layerData.map {
            onMapUpdate(it.map, it.mapUiState)
        }.launchIn(scope)
    }

    private suspend fun onMapUpdate(map: Map, mapUiState: MapUiState) {
        mapInteractor.getLandmarkPositions(map).map { (landmark, x, y) ->
            val id = "$landmarkPrefix-${UUID.randomUUID()}"
            val state = LandmarkState(id, landmark)
            mapUiState.mapState.addMarker(
                id,
                x,
                y,
                relativeOffset = Offset(-0.5f, -0.5f),
                zIndex = 1f
            ) {
                LandMark(Modifier.padding(5.dp), state.isStatic)
            }
            state
        }.associateBy { it.id }.also {
            landmarkListState = it
        }
    }

    override fun onMarkerTap(mapState: MapState, mapId: Int, id: String, x: Double, y: Double) {
        if (id.startsWith(markerGrabPrefix)) {
            onMarkerGrabTap(id, mapState)
            return
        }
        val landmarkState = landmarkListState[id] ?: return

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

            val calloutId = "$calloutPrefix-$id"
            mapState.addCallout(
                calloutId, x, y,
                relativeOffset = Offset(pos.relativeAnchorLeft, pos.relativeAnchorTop),
                absoluteOffset = Offset(pos.absoluteAnchorLeft, pos.absoluteAnchorTop),
                autoDismiss = true, clickable = false, zIndex = 3f
            ) {
                val subTitle = landmarkListState[id]?.landmark?.let {
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
                        mapState.removeMarker(id)
                        // TODO: implement real deletion
                    },
                    onMoveAction = {
                        mapState.removeCallout(calloutId)
                        mapState.updateMarkerClickable(id, false)
                        landmarkState.isStatic = false
                        attachMarkerGrab(id, x, y, mapState, landmarkState)
                    }
                )
            }
        }
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
        mapState.enableMarkerDrag(grabId) { _, x, y, dx, dy ->
            mapState.moveMarker(grabId, x + dx, y + dy)
            mapState.moveMarker(markerId, x + dx, y + dy)
        }
    }

    private fun onMarkerGrabTap(markerGrabId: String, mapState: MapState) {
        val markerId = markerGrabId.substringAfter('-')
        val landmarkState = landmarkListState[markerId] ?: return
        landmarkState.isStatic = true
        mapState.updateMarkerClickable(markerId, true)

        val landmarkInfo = mapState.getMarkerInfo(markerId)
        val landmark = landmarkListState[markerId]?.landmark
        if (landmarkInfo != null && landmark != null) {
            scope.launch {
                layerData.collect {
                    mapInteractor.updateAndSaveLandmark(
                        landmark,
                        it.map,
                        landmarkInfo.x,
                        landmarkInfo.y
                    )
                }
            }
        }
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
        markersSnapshotFlow.mapNotNull {
            val position = positionMarkerSnapshot ?: return@mapNotNull null
            computeDistanceForId(landmarksSnapshot, position)
        }


    private suspend fun computeDistanceForId(
        landmarks: List<MarkerDataSnapshot>,
        position: MarkerDataSnapshot
    ): kotlin.collections.Map<String, Double?> = withContext(Dispatchers.Default) {
        fun computeDistance(aX: Double, aY: Double, bX: Double, bY: Double): Double? {
            val lonLatA = getLonLat(aX, aY, map) ?: return null
            val lonLatB = getLonLat(bX, bY, map) ?: return null
            return distanceApprox(lonLatA[1], lonLatA[0], lonLatB[1], lonLatB[0])
        }

        delay(100)  // slow it down a bit
        landmarks.associate {
            it.id to computeDistance(position.x, position.y, it.x, it.y)
        }
    }
}

private const val landmarkPrefix = "landmark"
private const val calloutPrefix = "callout"
private const val markerGrabPrefix = "grabLandmark"

private data class LandmarkState(val id: String, val landmark: Landmark) {
    var isStatic by mutableStateOf(true)
}

private val df = DecimalFormat("#.####").apply {
    roundingMode = RoundingMode.CEILING
}

private const val landmarkCalloutWidthDp = 140
private const val landmarkCalloutHeightDp = 100
