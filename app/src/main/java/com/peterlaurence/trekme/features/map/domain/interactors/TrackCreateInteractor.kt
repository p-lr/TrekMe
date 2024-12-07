package com.peterlaurence.trekme.features.map.domain.interactors

import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.map.domain.dao.ExcursionRefDao
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.map.domain.core.getNormalizedCoordinates
import com.peterlaurence.trekme.features.map.domain.models.NormalizedPos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

class TrackCreateInteractor @Inject constructor(
    private val excursionRepository: ExcursionRepository,
    private val excursionRefDao: ExcursionRefDao,
    private val excursionDao: ExcursionDao
) {
    suspend fun createExcursion(title: String, geoRecord: GeoRecord, map: Map) : ExcursionRef? {
        val id = UUID.randomUUID().toString()
        excursionRepository.putExcursion(
            id = id,
            title = title,
            type = ExcursionType.Hike,
            geoRecord = geoRecord,
            description = "",
            isPathEditable = true
        )
        val excursion = excursionRepository.getExcursion(id)

        return if (excursion != null) {
            excursionRefDao.createExcursionRef(map, excursion)
        } else null
    }

    suspend fun getCurrentRelativeCoordinates(map: Map, excursionId: String): Flow<NormalizedPos> {
        val geoRecord = excursionRepository.getGeoRecord(excursionId) ?: return flowOf()

        return flow {
            geoRecord.routeGroups.firstOrNull()?.routes?.firstOrNull()?.routeMarkers?.forEach {
                val c = getNormalizedCoordinates(it.lat, it.lon, map.mapBounds, map.projection)
                emit(NormalizedPos(c[0], c[1]))
            }
        }
    }

    suspend fun saveGeoRecord(map: Map, excursionRef: ExcursionRef, geoRecord: GeoRecord) {
        excursionDao.updateGeoRecord(excursionRef.id, geoRecord)

        /* Remove then add back the ref, because previous screen requires a ref list change to
         * fetch the updated georecord. */
        map.excursionRefs.update {
            it - excursionRef
        }
        map.excursionRefs.update {
            it + excursionRef
        }
    }
}