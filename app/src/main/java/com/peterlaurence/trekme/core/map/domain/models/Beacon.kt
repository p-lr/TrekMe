package com.peterlaurence.trekme.core.map.domain.models

data class Beacon(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radius: Float = 50f,
    val comment: String = ""
)
