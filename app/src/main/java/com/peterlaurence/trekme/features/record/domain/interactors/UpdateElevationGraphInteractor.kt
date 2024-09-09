package com.peterlaurence.trekme.features.record.domain.interactors

import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.features.record.domain.repositories.ElevationRepository
import javax.inject.Inject

class UpdateElevationGraphInteractor @Inject constructor(
    private val repository: ElevationRepository,
    private val excursionRepository: ExcursionRepository
) {
    suspend fun updateElevationGraph(id: String) {
        val geoRecord = excursionRepository.getGeoRecord(id)
        if (geoRecord != null) {
            repository.update(id, geoRecord)
        } else {
            repository.reset()
        }
    }
}