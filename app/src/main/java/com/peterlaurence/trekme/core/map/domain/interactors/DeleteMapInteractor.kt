package com.peterlaurence.trekme.core.map.domain.interactors

import android.app.Application
import android.content.Intent
import com.peterlaurence.trekme.core.map.domain.dao.MapDeleteDao
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.UpdateSpec
import com.peterlaurence.trekme.features.mapcreate.app.service.download.DownloadService
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import javax.inject.Inject

class DeleteMapInteractor @Inject constructor(
    private val mapDeleteDao: MapDeleteDao,
    private val mapRepository: MapRepository,
    private val downloadRepository: DownloadRepository,
    private val app: Application
) {
    suspend fun deleteMap(map: Map) {
        /* If a repair or update for this map is pending, stop the download before removing the map */
        val downloadStatus = downloadRepository.status.value
        if (downloadStatus is DownloadRepository.Started && downloadStatus.downloadSpec is UpdateSpec) {
            if (downloadStatus.downloadSpec.map.id == map.id) {
                val intent = Intent(app, DownloadService::class.java)
                intent.action = DownloadService.stopAction
                app.startService(intent)
            }
        }
        mapRepository.deleteMap(map)
        mapDeleteDao.deleteMap(map)
    }
}