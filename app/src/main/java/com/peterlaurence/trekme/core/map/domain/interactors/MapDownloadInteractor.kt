package com.peterlaurence.trekme.core.map.domain.interactors

import android.app.Application
import android.net.Uri
import com.peterlaurence.trekme.core.map.domain.dao.MapDownloadDao
import com.peterlaurence.trekme.core.map.domain.dao.MapUpdateDataDao
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.wmts.domain.dao.TileStreamProviderDao
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.features.common.domain.interactors.ImportGeoRecordInteractor
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import java.time.Instant
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
    private val mapUpdateDataDao: MapUpdateDataDao,
    private val app: Application
) {

    suspend fun processDownloadSpec(
        spec: MapDownloadSpec,
        onProgress: (Int) -> Unit
    ) {
        val result = when (spec) {
            is NewDownloadSpec -> processNewDownloadSpec(spec, onProgress)
            is UpdateSpec -> processUpdateSpec(spec, onProgress)
        }

        if (result is MapDownloadResult.Success) {
            val map = result.map
            val missingTilesCount = result.missingTilesCount
            when (spec) {
                is NewDownloadSpec -> mapUpdateDataDao.setNewDownloadData(map, missingTilesCount)
                is UpdateSpec -> {
                    val date = Instant.now().epochSecond
                    if (spec.repairOnly) {
                        mapUpdateDataDao.setRepairData(map, missingTilesCount, date)
                    } else {
                        mapUpdateDataDao.setUpdateData(map, missingTilesCount, date)
                    }
                }
            }
        }
    }

    private suspend fun processUpdateSpec(
        spec: UpdateSpec,
        onProgress: (Int) -> Unit
    ): MapDownloadResult {
        val tileStreamProvider = tileStreamProviderDao.newTileStreamProvider(
            spec.creationData.mapSourceData
        ).getOrNull() ?: run {
            repository.postDownloadEvent(MissingApiError)
            return MapDownloadResult.Error(MissingApiError)
        }

        val result = mapDownloadDao.processUpdateSpec(
            spec,
            tileStreamProvider,
            onProgress = {
                /* Publish an application-wide event */
                repository.postDownloadEvent(MapUpdatePending(spec.map.id, it, spec.repairOnly))

                /* Report back the progression to the caller */
                onProgress(it)
            }
        )

        /* Notify that the update finished correctly. */
        repository.postDownloadEvent(MapUpdateFinished(spec.map.id, spec.repairOnly))

        return result
    }

    private suspend fun processNewDownloadSpec(
        spec: NewDownloadSpec,
        onProgress: (Int) -> Unit
    ): MapDownloadResult {
        val tileStreamProvider = tileStreamProviderDao.newTileStreamProvider(
            spec.source
        ).getOrNull() ?: run {
            repository.postDownloadEvent(MissingApiError)
            return MapDownloadResult.Error(MissingApiError)
        }

        val result = mapDownloadDao.processNewDownloadSpec(
            spec,
            tileStreamProvider,
            onProgress = {
                /* Publish an application-wide event */
                repository.postDownloadEvent(MapDownloadPending(it))

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

        /* Notify that the download finished correctly. */
        repository.postDownloadEvent(MapDownloadFinished(map.id))
    }
}