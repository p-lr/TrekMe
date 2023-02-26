package com.peterlaurence.trekme.core.excursion.domain.repository

import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionRef
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import javax.inject.Inject

class ExcursionRepository @Inject constructor() {
    fun getExcursion(ref: ExcursionRef): Excursion {
        TODO()
    }

    fun getGeoRecord(ref: ExcursionRef): GeoRecord {
        TODO()
    }
}