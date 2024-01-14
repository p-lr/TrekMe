package com.peterlaurence.trekme.core.map.domain.interactors

import android.app.Application
import android.net.Uri
import com.peterlaurence.trekme.core.map.domain.dao.MapDownloadDao
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.wmts.domain.dao.TileStreamProviderDao
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.features.common.domain.interactors.ImportGeoRecordInteractor
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import javax.inject.Inject

/**
 * This interactor is intended to be used from a service.
 */
class MapDownloadInteractor @Inject constructor(
    private val mapDownloadDao: MapDownloadDao,
    private val tileStreamProviderDao: TileStreamProviderDao,
    private val saveMapInteractor: SaveMapInteractor,
    private val repository: DownloadRepository,
    private val importGeoRecordInteractor: ImportGeoRecordInteractor,
    private val mapExcursionInteractor: MapExcursionInteractor,
    private val app: Application
) {

    suspend fun processDownloadSpec(
        spec: MapDownloadSpec,
        onProgress: (Int) -> Unit
    ): MapDownloadResult {
        val progressEvent = MapDownloadPending(0)
        val tileStreamProvider = tileStreamProviderDao.newTileStreamProvider(
            spec.source
        ).getOrNull() ?: return MapDownloadResult.Error(MissingApiError)

        val result = mapDownloadDao.processDownloadSpec(
            spec,
            tileStreamProvider,
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
                postProcess(result.map, spec.geoRecordUris, spec.excursionIds)
            }
        }

        return result
    }

    private suspend fun postProcess(map: Map, geoRecordUris: Set<Uri>, excursionIds: Set<String>) {
        saveMapInteractor.addAndSaveMap(map)

        excursionIds.forEach { id ->
            mapExcursionInteractor.createExcursionRef(map, id)
        }
        geoRecordUris.forEach { uri ->
            importGeoRecordInteractor.applyGeoRecordUriToMap(uri, app.contentResolver, map)
        }

        /* Notify that the download is finished correctly. */
        repository.postDownloadEvent(MapDownloadFinished(map.id))
    }
}