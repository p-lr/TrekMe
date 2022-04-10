package com.peterlaurence.trekme.core.map.maploader

import com.peterlaurence.trekme.core.map.*
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import java.util.*

class MapLoader {
    /**
     * All [Projection]s are registered here.
     */
    private val projectionHashMap = object : HashMap<String, Class<out Projection>>() {
        init {
            put(MercatorProjection.NAME, MercatorProjection::class.java)
            put(UniversalTransverseMercator.NAME, UniversalTransverseMercator::class.java)
        }
    }

    /**
     * Mutate the [Projection] of a given [Map].
     *
     * @return true on success, false if something went wrong.
     */
    fun mutateMapProjection(map: Map, projectionName: String): Boolean {
        val projectionType = projectionHashMap[projectionName] ?: return false
        try {
            val projection = projectionType.newInstance()
            map.projection = projection
        } catch (e: InstantiationException) {
            // wrong projection name
            return false
        } catch (e: IllegalAccessException) {
            return false
        } catch (e: ExceptionInInitializerError) {
            return false
        }

        return true
    }

    interface MapArchiveListUpdateListener {
        fun onMapArchiveListUpdate(mapArchiveList: List<MapArchive>)
    }
}
