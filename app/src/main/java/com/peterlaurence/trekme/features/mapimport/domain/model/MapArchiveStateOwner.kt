package com.peterlaurence.trekme.features.mapimport.domain.model

import kotlinx.coroutines.flow.StateFlow

interface MapArchiveStateOwner {
    val archivesFlow: StateFlow<List<MapArchive>>
}