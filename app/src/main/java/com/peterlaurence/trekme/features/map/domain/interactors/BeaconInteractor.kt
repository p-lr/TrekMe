package com.peterlaurence.trekme.features.map.domain.interactors

import com.peterlaurence.trekme.core.geotools.pointAtDistanceAndAngle
import com.peterlaurence.trekme.core.map.domain.models.Beacon
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.map.domain.core.getNormalizedCoordinates
import com.peterlaurence.trekme.di.ApplicationScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.update
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

class BeaconInteractor @Inject constructor(
    private val mapRepository: MapRepository,
    @ApplicationScope private val scope: CoroutineScope
) {
    /**
     * Save beacon (debounced).
     */
    fun saveBeacon(mapId: UUID, beacon: Beacon) {
        updateBeaconJob?.cancel()
        updateBeaconJob = scope.launch {
            delay(1000)
            val map = mapRepository.getMap(mapId) ?: return@launch

            map.beacons.update { formerList ->
                formerList.filter { it.id != beacon.id } + beacon
            }

            // TODO beacon: use dao to save the beacon
        }
    }

    suspend fun getBeaconRadiusInPx(beacon: Beacon, map: Map): Float {
        val (x, _) = getNormalizedCoordinates(beacon.lat, beacon.lon, map.mapBounds, map.projection)

        val (lat, lon) = withContext(Dispatchers.Default) {
            pointAtDistanceAndAngle(beacon.lat, beacon.lon, beacon.radius, 90f)
        }

        val (x2, _) = getNormalizedCoordinates(lat, lon, map.mapBounds, map.projection)

        /* Since we took the point with an angle of 90°, dy = 0 (at least for web-mercator projection).
         * Anyway, we're already making an approximation since the shape of a beacon should be an
         * ellipsis, not a circle. */
        val dx = abs(x2 - x)

        return (dx * map.widthPx).toFloat()
    }

    private var updateBeaconJob: Job? = null
}