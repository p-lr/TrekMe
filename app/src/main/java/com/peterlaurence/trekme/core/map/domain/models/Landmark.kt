package com.peterlaurence.trekme.core.map.domain.models

data class Landmark(
    var name: String,
    var lat: Double,
    var lon: Double,
    var comment: String = ""
)