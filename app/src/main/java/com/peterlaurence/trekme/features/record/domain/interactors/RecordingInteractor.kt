package com.peterlaurence.trekme.features.record.domain.interactors

import android.net.Uri
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecordExportFormat
import com.peterlaurence.trekme.core.settings.Settings
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class RecordingInteractor @Inject constructor(
    private val excursionDao: ExcursionDao,
    private val settings: Settings
) {
    suspend fun delete(ids: List<String>): Boolean {
        return excursionDao.deleteExcursions(ids)
    }

    suspend fun rename(id: String, newName: String): Boolean {
        return excursionDao.rename(id, newName)
    }

    suspend fun getGeoRecordUri(id: String): Uri? {
        val exportFormat = settings.getRecordingExportFormat().firstOrNull() ?: GeoRecordExportFormat.Gpx

        return excursionDao.getGeoRecordUri(id, format = exportFormat)
    }
}