package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.geotools.distanceApprox
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.features.map.domain.core.TrackVicinityVerifier
import com.peterlaurence.trekme.features.map.domain.core.getLonLatFromNormalizedCoordinate
import com.peterlaurence.trekme.features.map.domain.core.getNormalizedCoordinates
import com.peterlaurence.trekme.features.map.domain.models.TrackFollowServiceState
import com.peterlaurence.trekme.features.map.domain.repository.TrackFollowRepository
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.util.android.isBackgroundLocationGranted
import com.peterlaurence.trekme.util.android.isBatteryOptimized
import com.peterlaurence.trekme.util.android.isLocationEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.allPaths
import ovh.plrapps.mapcompose.api.getPathData
import ovh.plrapps.mapcompose.api.hasPath
import ovh.plrapps.mapcompose.api.isPathWithinRange
import ovh.plrapps.mapcompose.api.onPathClick
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.updatePath
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.state.MapState

class TrackFollowLayer(
    private val scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val trackFollowRepository: TrackFollowRepository,
    private val mapFeatureEvents: MapFeatureEvents,
    private val appContext: Context,
    private val appEventBus: AppEventBus,
    private val onTrackSelected: () -> Unit
) {
    private val trackFollowHighlightId = "track-followed-highlight"

    val ackBatteryOptSignal = Channel<Unit>(1)

    private val _events = Channel<Event>(1)
    val events = _events.receiveAsFlow()

    init {
        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                trackFollowRepository.serviceState.collect { state ->
                    when(state) {
                        is TrackFollowServiceState.Started -> {
                            if (map.id == state.mapId) {
                                /* Need to wait the corresponding path to be rendered in the route layer */
                                awaitPath(mapState, state.trackId)
                                highlightFollowedTrack(mapState, state.trackId)
                            }
                        }
                        TrackFollowServiceState.Stopped -> {
                            /* Nothing, on purpose */
                        }
                    }
                }
            }
        }

        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                mapFeatureEvents.trackFollowStopEvent.collect {
                    if (map.id == it.mapId) {
                        resetFollowedTrack(mapState, it.trackId)
                    }
                }
            }
        }
    }

    /**
     * 3 steps:
     * 1. Check that location is enabled and that battery optimization is disabled
     * 2. Make paths clickable and start the service upon path selection
     * 3. Check for background location permission
     */
    fun start() = scope.launch {
        /* Step 1 */
        checkLocationAndBatteryOpt()

        /* Step 2 */
        val (map, mapState) = dataStateFlow.firstOrNull() ?: return@launch
        _events.send(Event.SelectTrackToFollow)

        mapState.onPathClick { id, _, _ ->
            val pathData = mapState.getPathData(id)
            if (pathData != null) {
                onTrackSelected()
                startTrackFollowService(pathData, map, id)
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

        /* Step 3 */
        checkBackgroundLocationPerm()
    }

    private suspend fun checkLocationAndBatteryOpt() {
        /* Check location service. If disabled, no need to go further. */
        if (!isLocationEnabled(appContext)) {
            val msg = WarningMessage(
                title = appContext.getString(R.string.warning_title),
                msg = appContext.getString(R.string.location_disabled_warning)
            )
            appEventBus.postMessage(msg)
            return
        }

        /* Check battery optimization, and inform the user if needed */
        if (isBatteryOptimized(appContext)) {
            _events.send(Event.DisableBatteryOptSignal)
            /* Wait for the user to take action before continuing */
            ackBatteryOptSignal.receive()
        }
    }

    private suspend fun checkBackgroundLocationPerm() {
        if (!isBackgroundLocationGranted(appContext)) {
            _events.send(Event.BackgroundLocationNotGranted)
        } else {
            appEventBus.requestBackgroundLocation()
        }
    }

    private fun startTrackFollowService(pathData: PathData, map: Map, trackId: String) = scope.launch {
        /* Done this way, the TrackVicinityVerifier has no reference on this layer, so the view-model doesn't leak. */
        val vicinityVerifier = object : TrackVicinityVerifier {
            val trackFollowedId = "track-followed"
            val mapState = MapState(levelCount = 1, fullWidth = map.widthPx, fullHeight = map.heightPx).apply {
                addPath(trackFollowedId, pathData, simplify = 2f)
            }
            var pixelPerMeterThreshold: Double? = null

            init {
                ProcessLifecycleOwner.get().lifecycleScope.launch {
                    val latLonLeft = getLonLatFromNormalizedCoordinate(0.0, 0.5, map.projection, map.mapBounds)
                    val latLonRight = getLonLatFromNormalizedCoordinate(1.0, 0.5, map.projection, map.mapBounds)
                    val mapWidthInMeters = withContext(Dispatchers.Default) {
                        distanceApprox(latLonLeft[1], latLonLeft[0], latLonRight[1], latLonRight[0])
                    }

                    pixelPerMeterThreshold = map.widthPx.toDouble() / mapWidthInMeters
                }
            }

            override suspend fun isInVicinity(latitude: Double, longitude: Double, thresholdInMeters: Int): Boolean {
                val pixelThreshold = ((this.pixelPerMeterThreshold ?: return true) * thresholdInMeters).toInt()
                val normalized = getNormalizedCoordinates(latitude, longitude, map.mapBounds, map.projection)
                return withContext(Dispatchers.Default) {
                    mapState.isPathWithinRange(trackFollowedId, pixelThreshold, normalized[0], normalized[1])
                }
            }
        }

        trackFollowRepository.serviceData.send(TrackFollowRepository.ServiceData(vicinityVerifier, map.id, trackId))
        mapFeatureEvents.postStartTrackFollowService()
    }

    private fun highlightFollowedTrack(mapState: MapState, trackId: String) {
        mapState.updatePath(trackId, zIndex = 1f)
        val p = mapState.getPathData(trackId)
        if (p != null) {
            mapState.addPath(trackFollowHighlightId, p, color = Color.Black, clickable = true, zIndex = 0f, width = 6.dp)
        }
    }

    private fun resetFollowedTrack(mapState: MapState, trackId: String) {
        mapState.removePath(trackFollowHighlightId)
        mapState.updatePath(trackId, zIndex = 0f)
    }

    private suspend fun awaitPath(mapState: MapState, pathId: String) {
        var cnt = 0
        while (cnt < 3) {
            if (mapState.hasPath(pathId)) {
                return
            } else {
                delay(500)
                cnt++
            }
        }
    }

    sealed interface Event {
        object DisableBatteryOptSignal : Event
        object BackgroundLocationNotGranted : Event
        object SelectTrackToFollow : Event
    }
}