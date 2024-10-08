package com.peterlaurence.trekme.features.maplist.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.billing.domain.interactors.HasOneExtendedOfferInteractor
import com.peterlaurence.trekme.core.map.domain.dao.MapUpdateDataDao
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.interactors.*
import com.peterlaurence.trekme.core.map.domain.models.CalibrationMethod
import com.peterlaurence.trekme.core.map.domain.models.MapUpdatePending
import com.peterlaurence.trekme.core.map.domain.models.UpdateSpec
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.mapcreate.app.service.download.DownloadService
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.util.ResultL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

/**
 * The view-model for the map settings.
 *
 * @since 2020/08/14
 */
@HiltViewModel
class MapSettingsViewModel @Inject constructor(
    private val app: Application,
    private val mutateMapCalibrationInteractor: MutateMapCalibrationInteractor,
    private val renameMapInteractor: RenameMapInteractor,
    private val updateMapSizeInteractor: UpdateMapSizeInteractor,
    private val setMapThumbnailInteractor: SetMapThumbnailInteractor,
    private val archiveMapInteractor: ArchiveMapInteractor,
    private val mapRepository: MapRepository,
    private val mapUpdateDataDao: MapUpdateDataDao,
    private val downloadRepository: DownloadRepository,
    hasOneExtendedOfferInteractor: HasOneExtendedOfferInteractor,
) : ViewModel() {
    val mapFlow: StateFlow<Map?> = mapRepository.settingsMapFlow

    val mapSize: MutableStateFlow<ResultL<Long?>> = MutableStateFlow(
        ResultL.success(mapFlow.value?.sizeInBytes?.value)
    )

    private val _mapImageImportEvent = Channel<Boolean>(1)
    val mapImageImportEvent = _mapImageImportEvent.receiveAsFlow()

    val hasExtendedOffer = hasOneExtendedOfferInteractor.getPurchaseFlow(viewModelScope)

    /* This state reflects whether or not a map is currently being updated, either by a repair or
     * by a full update. */
    val mapUpdateStateFlow: StateFlow<MapUpdateState?> = channelFlow {
        downloadRepository.status.collectLatest { status ->
            when(status) {
                is DownloadRepository.UpdatingMap -> {
                    downloadRepository.downloadEvent.collect { event ->
                        val state = if (event is MapUpdatePending) {
                            MapUpdateState(event.mapId, event.progress / 100f, event.repairOnly)
                        } else null
                        send(state)
                    }
                }
                is DownloadRepository.DownloadingNewMap -> send(null)
                DownloadRepository.Stopped -> send(null)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        /* Lazy-load missing tiles count */
        viewModelScope.launch {
            mapFlow.collect { settingsMap ->
                if (settingsMap != null) {
                    mapUpdateDataDao.loadMapUpdateData(settingsMap)
                }
            }
        }
    }

    /**
     * Changes the thumbnail of a [Map].
     */
    fun setMapImage(mapId: UUID, uri: Uri) = viewModelScope.launch {
        val map = mapRepository.getMap(mapId) ?: return@launch
        setMapThumbnailInteractor.setMapThumbnail(map, uri)
    }

    /**
     * Changes the thumbnail of a [Map].
     */
    fun setMapImage(map: Map, uri: Uri) = viewModelScope.launch {
        setMapThumbnailInteractor.setMapThumbnail(map, uri).also { success ->
            _mapImageImportEvent.send(success)
        }
    }

    /**
     * Start zipping a map and write the zip archive to the directory defined by the provided [uri].
     * Internally uses a [Flow] which only emits distinct events. Those events are listened by the
     * main activity, because the user might leave this view ;
     * we want to reliably inform the user when this task is finished.
     */
    fun archiveMap(map: Map, uri: Uri) {
        archiveMapInteractor.archiveMap(map, uri)
    }

    fun renameMap(map: Map, newName: String) {
        viewModelScope.launch {
            renameMapInteractor.renameMap(map, newName)
        }
    }

    fun setCalibrationPointsNumber(map: Map, number: Int) {
        val newCalibrationMethod = when (number) {
            2 -> CalibrationMethod.SIMPLE_2_POINTS
            3 -> CalibrationMethod.CALIBRATION_3_POINTS
            4 -> CalibrationMethod.CALIBRATION_4_POINTS
            else -> CalibrationMethod.SIMPLE_2_POINTS
        }

        viewModelScope.launch {
            mutateMapCalibrationInteractor.mutateCalibrationMethod(map, newCalibrationMethod)
        }
    }

    fun setProjection(map: Map, projectionName: String?) = viewModelScope.launch {
        mutateMapCalibrationInteractor.mutateProjection(map, projectionName)
    }

    fun computeMapSize(map: Map) = viewModelScope.launch {
        mapSize.value = ResultL.loading()
        updateMapSizeInteractor.updateMapSize(map).onSuccess {
            mapSize.value = ResultL.success(it)
        }.onFailure {
            mapSize.value = ResultL.success(null)
        }
    }

    fun update(map: Map, repairOnly: Boolean) {
        val creationData = map.creationData
        if (creationData != null) {
            val repairSpec = UpdateSpec(map, creationData, repairOnly = repairOnly)
            downloadRepository.postMapDownloadSpec(repairSpec)
            val intent = Intent(app, DownloadService::class.java)
            app.startService(intent)
        }
    }

    /**
     * @param progress values are in the range [0.0 .. 1.0]
     */
    data class MapUpdateState(val mapId: UUID, val progress: Float, val repairOnly: Boolean)
}
