package com.peterlaurence.trekme.core.georecord.domain.repository

import android.net.Uri
import com.peterlaurence.trekme.core.georecord.domain.datasource.FileBasedSource
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecordLightWeight
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject

/**
 * Since [GeoRecord]s are potentially heavy objects, this repository only exposes a [StateFlow] of
 * [GeoRecordLightWeight].
 * However, it's still possible to get a single [GeoRecord] using [getGeoRecord].
 *
 * @since 2022/07/30
 */
class GeoRecordRepository @Inject constructor(
    private val fileBasedSource: FileBasedSource
) {
    /* For the moment, the repository only exposes the flow from the file-based source */
    fun getGeoRecordsFlow(): StateFlow<List<GeoRecordLightWeight>> {
        return fileBasedSource.getGeoRecordsFlow()
    }

    fun getUri(id: UUID): Uri? {
        return fileBasedSource.getUri(id)
    }

    suspend fun getGeoRecord(id: UUID): GeoRecord? {
        return fileBasedSource.getRecord(id)
    }

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