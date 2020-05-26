package com.peterlaurence.trekme.core.map

import com.peterlaurence.trekme.core.map.gson.MarkerGson

fun MarkerGson.Marker.getRelativeX(map: Map): Double {
    return if (map.projection != null) proj_x else lon
}

fun MarkerGson.Marker.getRelativeY(map: Map): Double {
    return if (map.projection != null) proj_y else lat
}