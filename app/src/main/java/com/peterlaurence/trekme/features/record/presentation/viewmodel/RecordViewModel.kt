package com.peterlaurence.trekme.features.record.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.georecord.domain.interactors.GeoRecordInteractor
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.features.common.domain.interactors.ImportGeoRecordInteractor
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
    private val geoRecordInteractor: GeoRecordInteractor,
    private val importGeoRecordInteractor: ImportGeoRecordInteractor,
    private val getMapInteractor: GetMapInteractor,
    private val mapExcursionInteractor: MapExcursionInteractor,
    private val restoreRecordInteractor: RestoreRecordInteractor,
    private val app: Application,
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

    fun importRecordInMap(mapId: UUID, recordId: UUID) = viewModelScope.launch {
        val map = getMapInteractor.getMap(mapId) ?: return@launch

        /* In this particular case, we need to know from inside the view-model the true nature of
         * the data behind "recordId" (an excursion or a georecord). */
        val excursionId = geoRecordInteractor.getExcursionId(recordId)
        if (excursionId != null) {
            mapExcursionInteractor.createExcursionRef(map, excursionId)
            _excursionImportEvent.send(true)
        } else {
            val uri = geoRecordInteractor.getRecordUri(recordId) ?: return@launch

            importGeoRecordInteractor.applyGeoRecordUriToMap(uri, app.contentResolver, map).let {
                geoRecordImportResultChannel.send(it)
            }
        }
    }
}