package com.peterlaurence.trekme.features.map.domain.interactors

import android.content.Context
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.geotools.pointAtDistanceAndAngle
import com.peterlaurence.trekme.core.map.domain.dao.BeaconDao
import com.peterlaurence.trekme.core.map.domain.models.Beacon
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.map.domain.core.getNormalizedCoordinates
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.features.map.domain.core.getLonLatFromNormalizedCoordinate
import com.peterlaurence.trekme.features.map.domain.models.BeaconWithNormalizedPos
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

class BeaconInteractor @Inject constructor(
    private val mapRepository: MapRepository,
    private val beaconDao: BeaconDao,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) {
    suspend fun getBeaconsFlow(map: Map): Flow<List<BeaconWithNormalizedPos>> {
        /* Import beacons */
        beaconDao.getBeaconsForMap(map)

        return map.beacons.map { beaconList ->
            beaconList.map { beacon ->
                val (x, y) = getNormalizedCoordinates(
                    beacon.lat,
                    beacon.lon,
                    map.mapBounds,
                    map.projection
                )

                BeaconWithNormalizedPos(beacon, x, y)
            }
        }
    }

    suspend fun makeBeacon(map: Map, x: Double, y: Double): Beacon {
        return withContext(scope.coroutineContext) {
            val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, map.mapBounds)
            val name = context.getString(R.string.beacon_default_name)
            // TODO fetch last radius from data store

            Beacon(id = UUID.randomUUID().toString(), name = name, lat = lonLat[1], lon = lonLat[0])
        }
    }

    /**
     * Update the marker position and save.
     * [x] and [y] are expected to be normalized coordinates.
     */
    suspend fun updateAndSaveBeacon(beacon: Beacon, map: Map, x: Double, y: Double): Beacon {
        val mapBounds = map.mapBounds

        val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, mapBounds)

        val updatedBeacon = beacon.copy(lat = lonLat[1], lon = lonLat[0])

        map.beacons.update { formerList ->
            formerList.filter { it.id != beacon.id } + updatedBeacon
        }

        scope.launch {
            beaconDao.saveBeacons(map)
        }

        return updatedBeacon
    }

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

            beaconDao.saveBeacons(map)
        }
    }

    fun deleteBeacon(beacon: Beacon, mapId: UUID) = scope.launch {
        val map = mapRepository.getMap(mapId) ?: return@launch

        map.beacons.update { formerList ->
            formerList.filter { it.id != beacon.id }
        }

        beaconDao.saveBeacons(map)
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