package com.peterlaurence.trekme.core.lib.geojson.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Feature(
    @SerialName("geometry")
    val geometry: Geometry,

    @SerialName("properties")
    val properties: Map<String, String> = emptyMap()
) {
    @SerialName("type")
    val type: String = "Feature"
}
