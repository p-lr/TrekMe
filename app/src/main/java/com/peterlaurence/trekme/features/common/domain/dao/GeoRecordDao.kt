package com.peterlaurence.trekme.features.common.domain.dao

import android.net.Uri
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import kotlinx.coroutines.flow.StateFlow
import java.util.*

interface GeoRecordDao {
    fun getGeoRecordsFlow(): StateFlow<List<GeoRecord>>
    fun getUri(id: UUID): Uri?
    suspend fun getRecord(id: UUID): GeoRecord?
    suspend fun importRecordingFromUri(uri: Uri): GeoRecord?
    suspend fun renameRecording(id: UUID, newName: String): Boolean
    suspend fun deleteRecordings(ids: List<UUID>): Boolean
}