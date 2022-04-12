package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import javax.inject.Inject

class MutateMapProjectionInteractor @Inject constructor(
    private val mapRepository: MapRepository
) {
    /**
     * Mutate the [Projection] of a given [Map].
     *
     * @return true on success, false if something went wrong.
     */
    fun mutateMapProjection(map: Map, projectionName: String?): Boolean {
        val projectionType = projectionHashMap[projectionName]
        return runCatching {
            val projection = projectionType?.newInstance()
            val oldConfig = map.configSnapshot
            val newConfig = oldConfig.copy(calibration = oldConfig.calibration?.copy(projection = projection))

            val newMap = map.copy(config = newConfig)
            mapRepository.notifyUpdate(map, newMap)
            true
        }.getOrDefault(false)
    }

    /**
     * All [Projection]s are registered here.
     */
    private val projectionHashMap = object : HashMap<String, Class<out Projection>>() {
        init {
            put(MercatorProjection.NAME, MercatorProjection::class.java)
            put(UniversalTransverseMercator.NAME, UniversalTransverseMercator::class.java)
        }
    }
}