package com.peterlaurence.trekme.features.mapimport.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchive
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchiveStateOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


@HiltViewModel
class MapImportViewModel @Inject constructor(
    mapArchiveStateOwner: MapArchiveStateOwner,
) : ViewModel() {

    val archives: StateFlow<List<MapArchive>> = mapArchiveStateOwner.archivesFlow
}

