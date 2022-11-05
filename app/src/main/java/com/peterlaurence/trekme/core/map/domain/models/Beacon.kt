package com.peterlaurence.trekme.core.map.domain.models

data class Beacon(
    val name: String,
    val lat: Double,
    val lon: Double,
    val radius: Int = 50,
    val comment: String = ""
)
