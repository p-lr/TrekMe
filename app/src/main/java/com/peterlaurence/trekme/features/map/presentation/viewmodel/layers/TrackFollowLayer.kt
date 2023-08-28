package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import android.content.Intent
import com.peterlaurence.trekme.core.geotools.distanceApprox
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.map.app.service.BeaconService
import com.peterlaurence.trekme.features.map.domain.core.TrackVicinityVerifier
import com.peterlaurence.trekme.features.map.domain.core.getLonLatFromNormalizedCoordinate
import com.peterlaurence.trekme.features.map.domain.core.getNormalizedCoordinates
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.allPaths
import ovh.plrapps.mapcompose.api.getPathData
import ovh.plrapps.mapcompose.api.isPathWithinRange
import ovh.plrapps.mapcompose.api.onPathClick
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.updatePath
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.state.MapState

class TrackFollowLayer(
    private val scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val mapFeatureEvents: MapFeatureEvents
) {
    private val trackFollowedId = "track-followed"

    fun start() = scope.launch {
        val (map, mapState) = dataStateFlow.firstOrNull() ?: return@launch

        mapState.onPathClick { id, _, _ ->
            val pathData = mapState.getPathData(id)
            if (pathData != null) {
                startTrackFollowService(pathData, map)
            }

            /* Now that selection is done, set paths to not clickable */
            mapState.allPaths { p ->
                updatePath(p.id, clickable = false)
            }
        }

        mapState.allPaths { p ->
            /* Some paths, like the live route, shouldn't be clickable */
            if (p.zIndex == 0f) {
                updatePath(p.id, clickable = true)
            }
        }
    }

    private fun startTrackFollowService(pathData: PathData, map: Map) = scope.launch {
        val mapState = MapState(levelCount = 1, fullWidth = map.widthPx, fullHeight = map.heightPx)
        mapState.addPath(trackFollowedId, pathData)

        val latLonLeft = getLonLatFromNormalizedCoordinate(0.0, 0.5, map.projection, map.mapBounds)
        val latLonRight = getLonLatFromNormalizedCoordinate(1.0, 0.5, map.projection, map.mapBounds)
        val mapWidthInMeters = withContext(Dispatchers.Default) {
            distanceApprox(latLonLeft[1], latLonLeft[0], latLonRight[1], latLonRight[0])
        }

        val pixelThreshold = (100 * map.widthPx / mapWidthInMeters).toInt()

        val vicinityVerifier = TrackVicinityVerifier { latitude, longitude ->
            val normalized = getNormalizedCoordinates(latitude, longitude, map.mapBounds, map.projection)
            mapState.isPathWithinRange(trackFollowedId, pixelThreshold, normalized[0], normalized[1])
        }

        mapFeatureEvents.trackVicinityVerifier.send(vicinityVerifier)
        mapFeatureEvents.postStartTrackFollowService()
    }
}