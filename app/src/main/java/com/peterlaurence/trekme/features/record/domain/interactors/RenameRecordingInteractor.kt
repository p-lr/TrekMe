package com.peterlaurence.trekme.features.record.domain.interactors

import com.peterlaurence.trekme.features.common.domain.repositories.GeoRecordRepository
import javax.inject.Inject

class RenameRecordingInteractor @Inject constructor(
    private val geoRecordRepository: GeoRecordRepository
) {
    suspend fun renameRecording(oldName: String, newName: String) {
        geoRecordRepository.renameRecording(oldName, newName)
    }
}