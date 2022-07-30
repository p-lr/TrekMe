package com.peterlaurence.trekme.core.georecord.domain.repository

import android.net.Uri
import com.peterlaurence.trekme.core.georecord.domain.datasource.FileBasedSource
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject

class GeoRecordRepository @Inject constructor(
    private val fileBasedSource: FileBasedSource
) {
    /* For the moment, the repository only exposes the flow from the file-based source */
    fun getGeoRecordsFlow(): StateFlow<List<GeoRecord>> {
        return fileBasedSource.getGeoRecordsFlow()
    }

    fun getUri(id: UUID): Uri? {
        return fileBasedSource.getUri(id)
    }

    suspend fun getGeoRecord(id: UUID): GeoRecord? {
        return fileBasedSource.getRecord(id)
    }

    /**
     * Resolves the given uri to an actual file, and copies it to the app's storage location for
     * recordings. Then, the copied file is parsed to get the corresponding [GeoRecord] instance.
     */
    suspend fun importGeoRecordFromUri(uri: Uri): GeoRecord? {
        return fileBasedSource.importGeoRecordFromUri(uri)
    }

    suspend fun renameGeoRecord(id: UUID, newName: String): Boolean {
        return fileBasedSource.renameGeoRecord(id, newName)
    }

    suspend fun deleteGeoRecords(ids: List<UUID>): Boolean {
        return fileBasedSource.deleteGeoRecords(ids)
    }
}