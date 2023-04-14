package com.peterlaurence.trekme.core.excursion.domain.repository

import android.net.Uri
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import javax.inject.Inject

class ExcursionRepository @Inject constructor(
    private val dao: ExcursionDao
) {
    suspend fun getExcursion(id: String): Excursion? {
        return dao.getExcursionsFlow().value.firstOrNull {
            it.id == id
        }
    }

    suspend fun getGeoRecordUri(id: String): Uri? {
        val excursion = getExcursion(id) ?: return null

        return dao.getGeoRecordUri(excursion)
    }
}