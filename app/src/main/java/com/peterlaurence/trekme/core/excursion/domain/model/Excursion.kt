package com.peterlaurence.trekme.core.excursion.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface Excursion {
    val id: String
    val title: MutableStateFlow<String>
    val type: ExcursionType
    val description: String
    val waypoints: StateFlow<List<ExcursionWaypoint>>  // waypoints are lazy loaded
    val photos: List<ExcursionPhoto>
    // TODO: maybe add statistics
}
