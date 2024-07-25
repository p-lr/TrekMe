package com.peterlaurence.trekme.core.lib.geojson.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A geospatial data interchange format compliant with [RFC-7946](https://datatracker.ietf.org/doc/html/rfc7946)
 */
@Serializable
class GeoJson(
    @SerialName("features")
    val features: List<Feature>
) {
    val type: String = "FeatureCollection"
}