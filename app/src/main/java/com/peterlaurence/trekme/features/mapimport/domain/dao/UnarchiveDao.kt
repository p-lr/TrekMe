package com.peterlaurence.trekme.features.mapimport.domain.dao

import com.peterlaurence.trekme.features.mapimport.domain.model.UnzipEvent
import kotlinx.coroutines.flow.Flow
import java.util.*

interface UnarchiveDao {
    suspend fun unarchive(id: UUID): Flow<UnzipEvent>
}