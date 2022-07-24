package com.peterlaurence.trekme.features.record.domain.interactors

import com.peterlaurence.trekme.features.common.domain.repositories.GeoRecordRepository
import java.util.*
import javax.inject.Inject

class RenameRecordingInteractor @Inject constructor(
    private val geoRecordRepository: GeoRecordRepository
) {
    suspend fun renameRecording(id: UUID, newName: String) {
        geoRecordRepository.renameRecording(id, newName)
    }
}