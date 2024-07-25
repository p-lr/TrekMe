package com.peterlaurence.trekme.core.lib.geojson.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Geometry {
    @Serializable
    @SerialName("Point")
    class Point(
        @SerialName("coordinates")
        val coordinates: List<Double>
    ) : Geometry()

    @Serializable
    @SerialName("MultiLineString")
    class MultiLineString(
        @SerialName("coordinates")
        val coordinates: List<List<List<Double>>>
    ) : Geometry()

    @SerialName("LineString")
    class LineString(
        @SerialName("coordinates")
        val coordinates: List<List<Double>>
    ) : Geometry()
}

