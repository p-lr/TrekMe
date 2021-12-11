package com.peterlaurence.trekme.core.map

import com.peterlaurence.trekme.core.map.domain.Marker

@Deprecated("To be removed after Compose refactor")
fun Marker.getRelativeX(map: Map): Double {
    return if (map.projection != null) proj_x ?: lon else lon
}

@Deprecated("To be removed after Compose refactor")
fun Marker.getRelativeY(map: Map): Double {
    return if (map.projection != null) proj_y ?: lat else lat
}