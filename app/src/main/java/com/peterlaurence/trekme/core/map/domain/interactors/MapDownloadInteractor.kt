package com.peterlaurence.trekme.core.map.domain.interactors

import android.app.Application
import android.net.Uri
import com.peterlaurence.trekme.core.map.domain.dao.MapDownloadDao
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.features.common.domain.interactors.ImportGeoRecordInteractor
import javax.inject.Inject

class MapDownloadInteractor @Inject constructor(
    private val mapDownloadDao: MapDownloadDao,
    private val saveMapInteractor: SaveMapInteractor,
    private val repository: DownloadRepository,
    private val importGeoRecordInteractor: ImportGeoRecordInteractor,
    private val app: Application
) {

    suspend fun processDownloadRequest(
        request: DownloadMapRequest,
        onProgress: (Int) -> Unit
    ): MapDownloadResult {
        val progressEvent = MapDownloadPending(0)

        val result = mapDownloadDao.processRequest(
            request,
            onProgress = {
                /* Publish an application-wide event */
                progressEvent.progress = it
                repository.postDownloadEvent(progressEvent)

                /* Report back the progression to the caller */
                onProgress(it)
            }
        )

        when (result) {
            is MapDownloadResult.Error -> {
                repository.postDownloadEvent(MapDownloadStorageError)
            }
            is MapDownloadResult.Success -> {
                postProcess(result.map, request.geoRecordUris)
            }
        }

        return result
    }

    private suspend fun postProcess(map: Map, geoRecordUris: Set<Uri>) {
        saveMapInteractor.addAndSaveMap(map)
        geoRecordUris.forEach { uri ->
            importGeoRecordInteractor.applyGeoRecordUriToMap(uri, app.contentResolver, map)
        }

        /* Notify that the download is finished correctly. */
        repository.postDownloadEvent(MapDownloadFinished(map.id))
    }
}