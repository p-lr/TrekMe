package com.peterlaurence.trekme.features.mapimport.domain.model

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.MapParseStatus
import java.util.*

sealed class UnzipEvent {
    abstract val archiveId: UUID
}

data class UnzipProgressEvent(override val archiveId: UUID, val p: Int) : UnzipEvent()
data class UnzipErrorEvent(override val archiveId: UUID) : UnzipEvent()
data class UnzipFinishedEvent(override val archiveId: UUID) : UnzipEvent()
data class UnzipMapImportedEvent(override val archiveId: UUID, val map: Map?, val status: MapParseStatus) : UnzipEvent()