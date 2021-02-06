package com.peterlaurence.trekme.util.gpx.model


data class Metadata(val name: String? = null, val time: Long? = null, val bounds: Bounds? = null,
                    val elevationSourceInfo: ElevationSourceInfo? = null)

/**
 * Defines an area. Conform to the GPX 1.1 specification.
 */
data class Bounds(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double)

/**
 * Container for elevation source's properties.
 */
data class ElevationSourceInfo(val elevationSource: ElevationSource, val sampling: Int)

enum class ElevationSource {
    GPS, IGN_RGE_ALTI
}