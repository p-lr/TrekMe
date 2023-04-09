package com.peterlaurence.trekme.core.excursion.domain.model

interface ExcursionWaypoint {
    val id: String
    val name: String
    val latitude: Double
    val longitude: Double
    val elevation: Double?
    val comment: String
    val photos: List<ExcursionPhoto>
}