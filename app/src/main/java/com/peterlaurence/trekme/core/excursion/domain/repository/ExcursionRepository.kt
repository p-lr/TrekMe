package com.peterlaurence.trekme.core.excursion.domain.repository

import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class ExcursionRepository @Inject constructor(
    private val dao: ExcursionDao
) {
    suspend fun getGeoRecord(id: String): GeoRecord? {
        val excursion = getExcursion(id) ?: return null

        return dao.getGeoRecord(excursion)
    }

    suspend fun getWaypoints(id: String): StateFlow<List<ExcursionWaypoint>>? {
        val excursion = getExcursion(id) ?: return null
        dao.initWaypoints(excursion)
        return excursion.waypoints
    }

    suspend fun updateWaypoint(excursionId: String, waypoint: ExcursionWaypoint, newLat: Double, newLon: Double) {
        val excursion = getExcursion(excursionId) ?: return
        dao.updateWaypoint(excursion, waypoint, newLat, newLon)
    }

    suspend fun updateWaypoint(excursionId: String, waypoint: ExcursionWaypoint, name: String?, lat: Double?, lon: Double?, comment: String?) {
        val excursion = getExcursion(excursionId) ?: return
        dao.updateWaypoint(excursion, waypoint, name, lat, lon, comment)
    }

    suspend fun deleteWaypoint(excursionId: String, waypoint: ExcursionWaypoint) {
        val excursion = getExcursion(excursionId) ?: return
        dao.deleteWaypoint(excursion, waypoint)
    }

    suspend fun putExcursion(id: String, title: String, type: ExcursionType, description: String, geoRecord: GeoRecord): PutExcursionResult {
        /* Check for a pending put with the same id */
        if (pendingPut.contains(id)) {
            return PutExcursionResult.Pending
        } else {
            pendingPut[id] = Unit
        }

        /* Check for an already existing excursion */
        dao.getExcursionsFlow().value.firstOrNull {
            it.id == id
        }?.also {
            pendingPut.remove(id)
            return PutExcursionResult.AlreadyExists
        }

        return try {
            if (dao.putExcursion(id, title, type, description, geoRecord)) {
                PutExcursionResult.Ok
            } else {
                PutExcursionResult.Error
            }
        } finally {
            pendingPut.remove(id)
        }
    }

    enum class PutExcursionResult {
        Ok, Pending, AlreadyExists, Error
    }

    suspend fun getExcursion(id: String): Excursion? {
        return dao.getExcursionsFlow().value.firstOrNull {
            it.id == id
        }
    }

    private val pendingPut = ConcurrentHashMap<String, Unit>()
}