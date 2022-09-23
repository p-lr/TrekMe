package com.peterlaurence.trekme.features.mapimport.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchive
import com.peterlaurence.trekme.features.mapimport.domain.repository.MapArchiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * This view-model manages [MapArchive]s.
 */
@HiltViewModel
class MapImportViewModel @Inject constructor(
    mapArchiveRepository: MapArchiveRepository,
) : ViewModel() {

    val archives: StateFlow<List<MapArchive>> = mapArchiveRepository.archivesFlow
}

