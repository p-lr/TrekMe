package com.peterlaurence.trekme.features.record.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.features.record.domain.interactors.RestoreRecordInteractor
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
) : ViewModel() {

    private val geoRecordImportResultChannel = Channel<GeoRecordImportResult>(1)
    val geoRecordImportResultFlow = geoRecordImportResultChannel.receiveAsFlow()

    private val _excursionImportEvent = Channel<Boolean>(1)
    val excursionImportEventFlow = _excursionImportEvent.receiveAsFlow()

    private val _geoRecordRecoverChannel = Channel<Unit>(1)
    val geoRecordRecoverEventFlow = _geoRecordRecoverChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (restoreRecordInteractor.hasRecordToRestore()) {
                _geoRecordRecoverChannel.send(Unit)
                restoreRecordInteractor.recoverRecord()
            }
        }
    }

    fun importRecordInMap(mapId: UUID, recordId: String) = viewModelScope.launch {
        val map = getMapInteractor.getMap(mapId) ?: return@launch

        mapExcursionInteractor.createExcursionRef(map, recordId)
        _excursionImportEvent.send(true)
    }
}