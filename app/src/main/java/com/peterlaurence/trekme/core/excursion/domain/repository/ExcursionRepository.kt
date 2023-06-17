package com.peterlaurence.trekme.core.excursion.domain.repository

import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class ExcursionRepository @Inject constructor(
    private val dao: ExcursionDao
) {
    suspend fun getGeoRecord(id: String): GeoRecord? {
        val excursion = getExcursion(id) ?: return null

        return dao.getGeoRecord(excursion)
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