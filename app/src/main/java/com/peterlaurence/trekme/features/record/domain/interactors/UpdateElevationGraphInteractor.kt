package com.peterlaurence.trekme.features.record.domain.interactors

import com.peterlaurence.trekme.core.georecord.domain.interactors.GeoRecordInteractor
import com.peterlaurence.trekme.features.record.domain.repositories.ElevationRepository
import java.util.*
import javax.inject.Inject

class UpdateElevationGraphInteractor @Inject constructor(
    private val repository: ElevationRepository,
    private val geoRecordInteractor: GeoRecordInteractor
) {
    suspend fun updateElevationGraph(id: UUID) {
        val geoRecord = geoRecordInteractor.getRecord(id)
        if (geoRecord != null) {
            repository.update(geoRecord)
        } else {
            repository.reset()
        }
    }
}