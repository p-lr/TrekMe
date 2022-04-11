package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import javax.inject.Inject

class MutateMapProjectionInteractor @Inject constructor() {
    /**
     * Mutate the [Projection] of a given [Map].
     *
     * @return true on success, false if something went wrong.
     */
    fun mutateMapProjection(map: Map, projectionName: String): Boolean {
        val projectionType = projectionHashMap[projectionName] ?: return false
        return runCatching {
            val projection = projectionType.newInstance()
            map.projection = projection
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