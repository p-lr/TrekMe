package com.peterlaurence.trekme.core.georecord.domain.dao

import android.net.Uri
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecordLightWeight
import kotlinx.coroutines.flow.StateFlow
import java.util.*

interface GeoRecordDao {
    fun getGeoRecordsFlow(): StateFlow<List<GeoRecordLightWeight>>
    fun getUri(id: UUID): Uri?
    suspend fun getRecord(id: UUID): GeoRecord?
    suspend fun importGeoRecordFromUri(uri: Uri): GeoRecord?
    suspend fun updateGeoRecord(geoRecord: GeoRecord): Boolean
    suspend fun renameGeoRecord(id: UUID, newName: String): Boolean
    suspend fun deleteGeoRecords(ids: List<UUID>): Boolean
}