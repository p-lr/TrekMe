package com.peterlaurence.trekme.core.map.domain

data class Landmark(
    var name: String,
    var lat: Double,
    var lon: Double,
    @Deprecated("To be removed after Compose refactor")
    var proj_x: Double?,
    @Deprecated("To be removed after Compose refactor")
    var proj_y: Double?,
    var comment: String
)