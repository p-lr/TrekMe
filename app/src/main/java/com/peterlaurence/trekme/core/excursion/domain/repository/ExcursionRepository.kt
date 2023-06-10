package com.peterlaurence.trekme.core.excursion.domain.repository

import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import javax.inject.Inject

class ExcursionRepository @Inject constructor(
    private val dao: ExcursionDao
) {
    private suspend fun getExcursion(id: String): Excursion? {
        return dao.getExcursionsFlow().value.firstOrNull {
            it.id == id
        }
    }

    suspend fun getGeoRecord(id: String): GeoRecord? {
        val excursion = getExcursion(id) ?: return null

        return dao.getGeoRecord(excursion)
    }

    suspend fun putExcursion(id: String, title: String, type: ExcursionType, description: String, geoRecord: GeoRecord): Boolean {
        // TODO: check existing excursion before creating a new one
        return dao.putExcursion(id, title, type, description, geoRecord)
    }
}