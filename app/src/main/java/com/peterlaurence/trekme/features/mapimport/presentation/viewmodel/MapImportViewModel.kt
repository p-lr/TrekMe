package com.peterlaurence.trekme.features.mapimport.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.features.mapimport.domain.interactor.MapArchiveInteractor
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchive
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchiveStateOwner
import com.peterlaurence.trekme.features.mapimport.domain.model.UnzipEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MapImportViewModel @Inject constructor(
    mapArchiveStateOwner: MapArchiveStateOwner,
    private val mapArchiveInteractor: MapArchiveInteractor
) : ViewModel() {

    val archives: StateFlow<List<MapArchive>> = mapArchiveStateOwner.archivesFlow
    var isImporting = MutableStateFlow(false)

    fun seekArchives(uri: Uri) = viewModelScope.launch {
        isImporting.value = true
        mapArchiveInteractor.seekArchivesAtLocation(uri)
        isImporting.value = false
    }

    suspend fun unArchive(mapArchive: MapArchive): Flow<UnzipEvent> {
        return mapArchiveInteractor.unarchiveAndGetEvents(mapArchive)
    }
}

