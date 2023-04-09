package com.peterlaurence.trekme.core.excursion.domain.repository

import android.net.Uri
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionRef
import javax.inject.Inject

class ExcursionRepository @Inject constructor(
    private val dao: ExcursionDao
) {
    suspend fun getExcursion(ref: ExcursionRef): Excursion? {
        return dao.getExcursionsFlow().value.firstOrNull {
            it.id == ref.id
        }
    }

    suspend fun getGeoRecordUri(ref: ExcursionRef): Uri? {
        val excursion = getExcursion(ref) ?: return null

        return dao.getGeoRecordUri(excursion)
    }
}