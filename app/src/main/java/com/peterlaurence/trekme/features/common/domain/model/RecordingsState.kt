package com.peterlaurence.trekme.features.common.domain.model

import com.peterlaurence.trekme.features.record.domain.model.RecordingData

sealed interface RecordingsState
data class RecordingsAvailable(val recordings: List<RecordingData>): RecordingsState
object Loading: RecordingsState