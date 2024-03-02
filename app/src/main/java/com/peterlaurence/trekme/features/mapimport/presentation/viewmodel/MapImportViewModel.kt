package com.peterlaurence.trekme.features.mapimport.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.domain.models.MapParseStatus
import com.peterlaurence.trekme.features.mapimport.domain.interactor.MapArchiveInteractor
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchive
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchiveStateOwner
import com.peterlaurence.trekme.features.mapimport.domain.model.UnzipEvent
import com.peterlaurence.trekme.features.mapimport.domain.model.UnzipMapImportedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class MapImportViewModel @Inject constructor(
    mapArchiveStateOwner: MapArchiveStateOwner,
    private val mapArchiveInteractor: MapArchiveInteractor
) : ViewModel() {

    val archives: StateFlow<List<MapArchive>> = mapArchiveStateOwner.archivesFlow
    var isImporting = MutableStateFlow(false)
    val archivesUiState = MutableStateFlow<List<MapArchiveUiState>>(emptyList())

    private val _importSuccessChannel = Channel<Unit>(1)
    val importSuccessEvent = _importSuccessChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            archives.collect {
                archivesUiState.value = it.map { mapArchive ->
                    MapArchiveUiState(mapArchive.id, mapArchive.name, MutableStateFlow(null))
                }
            }
        }
    }

    fun seekArchives(uri: Uri) = viewModelScope.launch {
        isImporting.value = true
        mapArchiveInteractor.seekArchivesAtLocation(uri)
        isImporting.value = false
    }

    fun unArchive(mapArchiveId: UUID) = viewModelScope.launch {
        val uiState = archivesUiState.value.firstOrNull { it.id == mapArchiveId } ?: return@launch
        val mapArchive = archives.value.firstOrNull { it.id == mapArchiveId } ?: return@launch
        mapArchiveInteractor.unarchiveAndGetEvents(mapArchive).collect {
            uiState.unzipEvent.value = it

            if (it is UnzipMapImportedEvent && (it.status == MapParseStatus.NEW_MAP || it.status == MapParseStatus.EXISTING_MAP)) {
                _importSuccessChannel.send(Unit)
            }
        }
    }

    suspend fun unArchiveOld(mapArchive: MapArchive): Flow<UnzipEvent> {
        return mapArchiveInteractor.unarchiveAndGetEvents(mapArchive)
    }
}

data class MapArchiveUiState(
    val id: UUID,
    val name: String,
    val unzipEvent: MutableStateFlow<UnzipEvent?>
)

