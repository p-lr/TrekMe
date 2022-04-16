package com.peterlaurence.trekme.core.map.domain.models

data class Landmark(
    var name: String,
    var lat: Double,
    var lon: Double,
    @Deprecated("To be removed after Compose refactor")
    var proj_x: Double? = null,
    @Deprecated("To be removed after Compose refactor")
    var proj_y: Double? = null,
    var comment: String = ""
)