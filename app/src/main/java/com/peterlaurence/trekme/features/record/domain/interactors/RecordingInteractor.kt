package com.peterlaurence.trekme.features.record.domain.interactors

import android.net.Uri
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.features.common.domain.repositories.GeoRecordRepository
import java.util.*
import javax.inject.Inject

class RecordingInteractor @Inject constructor(
    private val repository: GeoRecordRepository
) {
    fun getRecordUri(id: UUID): Uri? {
        return repository.getRecordUri(id)
    }

    suspend fun getRecord(id: UUID): GeoRecord? {
        return repository.getRecord(id)
    }
}