package com.peterlaurence.trekme.features.maplist.presentation.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.interactors.*
import com.peterlaurence.trekme.core.map.domain.models.CalibrationMethod
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.maplist.presentation.events.*
import com.peterlaurence.trekme.util.stackTraceAsString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

/**
 * The view-model for the MapSettingsFragment.
 *
 * @author P.Laurence on 14/08/20
 */
@HiltViewModel
class MapSettingsViewModel @Inject constructor(
    val app: Application,
    private val mutateMapCalibrationInteractor: MutateMapCalibrationInteractor,
    private val renameMapInteractor: RenameMapInteractor,
    private val updateMapSizeInteractor: UpdateMapSizeInteractor,
    private val setMapThumbnailInteractor: SetMapThumbnailInteractor,
    private val archiveMapInteractor: ArchiveMapInteractor,
    private val mapRepository: MapRepository
) : ViewModel() {

    private val _mapImageImportEvents =
        MutableSharedFlow<MapImageImportResult>(0, 1, BufferOverflow.DROP_OLDEST)
    val mapImageImportEvents = _mapImageImportEvents.asSharedFlow()

    val mapFlow: StateFlow<Map?> = mapRepository.settingsMapFlow

    /**
     * Changes the thumbnail of a [Map].
     */
    fun setMapImage(mapId: UUID, uri: Uri) = viewModelScope.launch {
        val map = mapRepository.getMap(mapId) ?: return@launch

        try {
            setMapThumbnailInteractor.setMapThumbnail(map, uri).onSuccess {
                _mapImageImportEvents.tryEmit(MapImageImportResult(true))
            }.onFailure {
                _mapImageImportEvents.tryEmit(MapImageImportResult(false))
            }
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceAsString())
            _mapImageImportEvents.tryEmit(MapImageImportResult(false))
        }
    }

    /**
     * Start zipping a map and write the zip archive to the directory defined by the provided [uri].
     * Internally uses a [Flow] which only emits distinct events. Those events are listened by the
     * main activity, and not the [MapSettingsFragment], because the user might leave this view ;
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

    fun setCalibrationPointsNumber(map: Map, numberStr: String?) {
        val newCalibrationMethod = when (numberStr) {
            "2" -> CalibrationMethod.SIMPLE_2_POINTS
            "3" -> CalibrationMethod.CALIBRATION_3_POINTS
            "4" -> CalibrationMethod.CALIBRATION_4_POINTS
            else -> CalibrationMethod.SIMPLE_2_POINTS
        }

        viewModelScope.launch {
            mutateMapCalibrationInteractor.mutateCalibrationMethod(map, newCalibrationMethod)
        }
    }

    fun setProjection(map: Map, projectionName: String?) = viewModelScope.launch {
        mutateMapCalibrationInteractor.mutateProjection(map, projectionName)
    }

    suspend fun computeMapSize(map: Map): Long? {
        return updateMapSizeInteractor.updateMapSize(map).getOrNull()
    }
}

private const val TAG = "MapSettingsViewModel.kt"