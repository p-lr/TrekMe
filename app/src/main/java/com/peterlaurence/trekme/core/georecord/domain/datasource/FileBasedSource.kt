package com.peterlaurence.trekme.core.georecord.domain.datasource

import android.net.Uri
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import kotlinx.coroutines.flow.StateFlow
import java.util.*

interface FileBasedSource {
    fun getGeoRecordsFlow(): StateFlow<List<GeoRecord>>
    fun getUri(id: UUID): Uri?
    suspend fun getRecord(id: UUID): GeoRecord?
    suspend fun importGeoRecordFromUri(uri: Uri): GeoRecord?
    suspend fun renameGeoRecord(id: UUID, newName: String): Boolean
    suspend fun deleteGeoRecords(ids: List<UUID>): Boolean
}