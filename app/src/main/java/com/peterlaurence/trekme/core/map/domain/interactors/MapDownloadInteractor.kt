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
import java.util.UUID
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
        spec: NewDownloadSpec,
        onStart: (UUID) -> Unit,
        onProgress: (Int) -> Unit
    ) {
        val result = onNewDownloadSpec(spec, onStart, onProgress)

        if (result is MapDownloadResult.Success) {
            val map = result.map
            val missingTilesCount = result.missingTilesCount
            mapUpdateDataDao.setNewDownloadData(map, missingTilesCount)
        }
    }

    suspend fun processUpdateSpec(
        spec: UpdateSpec,
        onProgress: (Int) -> Unit
    ) {
        val result = onUpdateSpec(spec, onProgress)

        if (result is MapDownloadResult.Success) {
            val map = result.map
            val missingTilesCount = result.missingTilesCount
            val date = Instant.now().epochSecond
            if (spec.repairOnly) {
                mapUpdateDataDao.setRepairData(map, missingTilesCount, date)
            } else {
                mapUpdateDataDao.setUpdateData(map, missingTilesCount, date)
            }
        }
    }

    private suspend fun onUpdateSpec(
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

    private suspend fun onNewDownloadSpec(
        spec: NewDownloadSpec,
        onStart: (UUID) -> Unit,
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
            onMapCreated = { map ->
                onStart(map.id)
                onMapCreated(map, spec.geoRecordUris, spec.excursionIds)
            },
            onProgress = {
                /* Publish an application-wide event */
                repository.postDownloadEvent(MapDownloadPending(it))

                /* Report back the progression to the caller */
                onProgress(it)
            }
        )

        when (result) {
            is MapDownloadResult.Success -> {
                postProcess(result.map)
            }
            is MapDownloadResult.Error -> {
                repository.postDownloadEvent(MapDownloadStorageError)
            }
        }

        return result
    }

    private suspend fun onMapCreated(map: Map, geoRecordUris: Set<Uri>, excursionIds: Set<String>) {
        saveMapInteractor.addAndSaveMap(map)

        excursionIds.forEach { id ->
            mapExcursionInteractor.createExcursionRef(map, id)
        }
        geoRecordUris.forEach { uri ->
            importGeoRecordInteractor.applyGeoRecordUriToMap(uri, app.contentResolver, map)
        }
    }

    private fun postProcess(map: Map) {
        /* Notify that the download finished correctly. */
        repository.postDownloadEvent(MapDownloadFinished(map.id))
    }
}