package com.peterlaurence.trekme.features.map.domain.interactors

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.map.domain.dao.ExcursionRefDao
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map
import java.util.UUID
import javax.inject.Inject

class TrackCreateInteractor @Inject constructor(
    private val excursionRepository: ExcursionRepository,
    private val excursionRefDao: ExcursionRefDao,
) {
    suspend fun createExcursion(title: String, geoRecord: GeoRecord, map: Map) : ExcursionRef? {
        val id = UUID.randomUUID().toString()
        excursionRepository.putExcursion(
            id = id,
            title = title,
            type = ExcursionType.Hike,
            geoRecord = geoRecord,
            description = ""
        )
        val excursion = excursionRepository.getExcursion(id)

        return if (excursion != null) {
            excursionRefDao.createExcursionRef(map, excursion)
        } else null
    }
}