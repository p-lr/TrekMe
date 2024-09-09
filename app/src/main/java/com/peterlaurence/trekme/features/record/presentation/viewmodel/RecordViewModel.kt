package com.peterlaurence.trekme.features.record.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.contains
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.record.domain.interactors.RestoreRecordInteractor
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordListEvent.NoMapContainingRecord
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordListEvent.RecordImport
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordListEvent.RecordRecover
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordListEvent.ShowCurrentMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private var openInMapScope = makeScope()
    private val mutex = Mutex()

    init {
        viewModelScope.launch {
            if (restoreRecordInteractor.hasRecordToRestore()) {
                _events.send(RecordRecover)
                restoreRecordInteractor.recoverRecord()
            }
        }
    }

    fun onResumed() {
        openInMapScope = makeScope()
    }

    private fun makeScope() = CoroutineScope(Job(viewModelScope.coroutineContext.job))

    fun importRecordInMap(mapId: UUID, recordId: String, boundingBox: BoundingBox) =
        viewModelScope.launch {
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

    fun openMapForBoundingBox(
        boundingBox: BoundingBox,
        recordId: String
    ) = openInMapScope.launch {
        /* Ensure that only one call is executed at a time. */
        mutex.withLock {
            openMapForBoundingBoxGuarded(boundingBox, recordId)
        }
    }

    private suspend fun openMapForBoundingBoxGuarded(boundingBox: BoundingBox, recordId: String) {
        val mapList = getMapInteractor.getMapList()

        val containingMaps = mutableListOf<Map>()
        for (map in mapList) {
            if (map.contains(boundingBox)) {
                mapExcursionInteractor.importExcursions(map)
                val ref = map.excursionRefs.value.firstOrNull { it.id == recordId }
                if (ref != null) {
                    mapExcursionInteractor.setVisibility(map, ref, visibility = true)
                    openMapForBoundingBox(boundingBox, map)

                    /* Cancelling the scope as we're navigating out, to make sure this method isn't
                     * invoked twice. The next time the record list is shown, the scope is
                     * re-created. */
                    openInMapScope.cancel()
                    return
                } else {
                    containingMaps.add(map)
                }
            }
        }

        if (containingMaps.isNotEmpty()) {
            val map = containingMaps.first()
            mapExcursionInteractor.createExcursionRef(map, recordId)
            openMapForBoundingBox(boundingBox, map)
            /* See comment above about openInMapScope.cancel() */
            openInMapScope.cancel()
        } else {
            _events.send(NoMapContainingRecord)
        }
    }

    private suspend fun openMapForBoundingBox(boundingBox: BoundingBox, map: Map) {
        _events.send(ShowCurrentMap)
        mapRepository.setCurrentMap(map)
        mapFeatureEvents.postGoToBoundingBox(map.id, boundingBox)
    }
}

sealed interface RecordListEvent {
    data class RecordImport(val recordId: String, val boundingBox: BoundingBox) : RecordListEvent
    data object RecordRecover : RecordListEvent
    data object ShowCurrentMap : RecordListEvent
    data object NoMapContainingRecord : RecordListEvent
}