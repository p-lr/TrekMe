package com.peterlaurence.trekme.features.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.models.Beacon
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

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

    private var updateBeaconJob: Job? = null
}