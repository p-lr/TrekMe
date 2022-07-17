package com.peterlaurence.trekme.features.record.domain.interactors

import com.peterlaurence.trekme.features.common.domain.repositories.GeoRecordRepository
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import javax.inject.Inject

class DeleteRecordingInteractor @Inject constructor(
    private val geoRecordRepository: GeoRecordRepository
) {
    suspend fun deleteRecording(recordingDataList: List<RecordingData>): Boolean {
        return geoRecordRepository.deleteRecordings(recordingDataList)
    }
}