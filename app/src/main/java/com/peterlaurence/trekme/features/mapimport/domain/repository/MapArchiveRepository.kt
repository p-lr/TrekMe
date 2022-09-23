package com.peterlaurence.trekme.features.mapimport.domain.repository

import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchive
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchiveStateOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapArchiveRepository @Inject constructor() : MapArchiveStateOwner {
    override val archivesFlow: StateFlow<List<MapArchive>> = MutableStateFlow(emptyList())

    fun setArchives(archives: List<MapArchive>) {
        (archivesFlow as MutableStateFlow).value = archives
    }
}