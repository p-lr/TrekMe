package com.peterlaurence.trekme.core.map.domain.models

import java.util.*

data class Landmark(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val lat: Double,
    val lon: Double,
    val comment: String = ""
)