package com.peterlaurence.trekme.features.record.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.map.domain.models.contains
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.record.domain.interactors.RestoreRecordInteractor
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordListEvent.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class RecordViewModel @Inject constructor(
    private val getMapInteractor: GetMapInteractor,
    private val mapExcursionInteractor: MapExcursionInteractor,
    private val restoreRecordInteractor: RestoreRecordInteractor,
    private val mapFeatureEvents: MapFeatureEvents,
    private val mapRepository: MapRepository
) : ViewModel() {
    private val _events = Channel<RecordListEvent>(1)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (restoreRecordInteractor.hasRecordToRestore()) {
                _events.send(RecordRecover)
                restoreRecordInteractor.recoverRecord()
            }
        }
    }

    fun importRecordInMap(mapId: UUID, recordId: String, boundingBox: BoundingBox) = viewModelScope.launch {
        val map = getMapInteractor.getMap(mapId) ?: return@launch

        val ref = map.excursionRefs.value.firstOrNull { it.id == recordId }
        if (ref != null) {
            mapExcursionInteractor.setVisibility(map, ref, visibility = true)
        } else {
            mapExcursionInteractor.createExcursionRef(map, recordId)
        }

        _events.send(RecordImport(recordId, boundingBox))
    }

    suspend fun hasContainingMap(boundingBox: BoundingBox): Boolean {
        val mapList = getMapInteractor.getMapList()
        return mapList.any { it.contains(boundingBox) }
    }

    fun openMapForBoundingBox(boundingBox: BoundingBox, recordId: String) = viewModelScope.launch {
        val mapList = getMapInteractor.getMapList()

        val containingMaps = mutableListOf<Map>()
        for (map in mapList) {
            if (map.contains(boundingBox)) {
                val ref = map.excursionRefs.value.firstOrNull { it.id == recordId }
                if (ref != null) {
                    mapExcursionInteractor.setVisibility(map, ref, visibility = true)
                    openMapForBoundingBox(boundingBox, map)
                    return@launch
                } else {
                    containingMaps.add(map)
                }
            }
        }

        if (containingMaps.isNotEmpty()) {
            val map = containingMaps.first()
            mapExcursionInteractor.createExcursionRef(map, recordId)
            openMapForBoundingBox(boundingBox, map)
        } else {
            _events.send(NoMapContainingRecord)
        }
    }

    private suspend fun openMapForBoundingBox(boundingBox: BoundingBox, map: Map) {
        mapRepository.setCurrentMap(map)
        _events.send(ShowCurrentMap)
        mapFeatureEvents.postGoToBoundingBox(map.id, boundingBox)
    }
}

sealed interface RecordListEvent {
    data class RecordImport(val recordId: String, val boundingBox: BoundingBox) : RecordListEvent
    data object RecordRecover : RecordListEvent
    data object ShowCurrentMap : RecordListEvent
    data object NoMapContainingRecord : RecordListEvent
}