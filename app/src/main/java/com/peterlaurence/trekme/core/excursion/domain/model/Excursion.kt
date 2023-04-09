package com.peterlaurence.trekme.core.excursion.domain.model

interface Excursion {
    val id: String
    val title: String
    val type: ExcursionType
    val description: String
    val photos: List<ExcursionPhoto>
    // TODO: maybe add statistics
}
