package com.peterlaurence.trekme.core.georecord.domain.interactors

import android.net.Uri
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.repository.GeoRecordRepository
import java.util.*
import javax.inject.Inject

class GeoRecordInteractor @Inject constructor(
    private val repository: GeoRecordRepository,
) {
    fun getRecordUri(id: UUID): Uri? {
        return repository.getUri(id)
    }

    suspend fun getRecord(id: UUID): GeoRecord? {
        return repository.getGeoRecord(id)
    }

    suspend fun delete(ids: List<UUID>): Boolean {
        return repository.deleteGeoRecords(ids)
    }

    suspend fun rename(id: UUID, newName: String) {
        repository.renameGeoRecord(id, newName)
    }

    suspend fun import(uriList: List<Uri>): Int {
        return uriList.count { uri ->
            repository.importGeoRecordFromUri(uri) != null
        }
    }

    fun getExcursionId(id: UUID): String? {
        return repository.getExcursionId(id)
    }

    fun getExcursionIds(ids: List<UUID>): List<String> {
        return repository.getExcursionIds(ids)
    }
}