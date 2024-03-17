package com.peterlaurence.trekme.features.record.domain.interactors

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.features.record.domain.model.RecordRestorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class RestoreRecordInteractor @Inject constructor(
    private val restorer: RecordRestorer,
    private val excursionRepository: ExcursionRepository,
    @ApplicationScope
    private val coroutineScope: CoroutineScope
) {
    suspend fun hasRecordToRestore(): Boolean {
        return restorer.hasRecordToRestore()
    }

    suspend fun recoverRecord(): Boolean {
        val (geoRecord, _) = restorer.restore() ?: return false

        val excursionId = UUID.randomUUID().toString()
        val result = excursionRepository.putExcursion(
            id = excursionId,
            title = geoRecord.name,
            type = ExcursionType.Hike,
            description = "",
            geoRecord = geoRecord
        )

        val success = when (result) {
            ExcursionRepository.PutExcursionResult.Ok, ExcursionRepository.PutExcursionResult.AlreadyExists -> {
                true
            }

            else -> false
        }

        if (success) {
            coroutineScope.launch {
                restorer.deleteRecord()
            }
        }

        return success
    }
}