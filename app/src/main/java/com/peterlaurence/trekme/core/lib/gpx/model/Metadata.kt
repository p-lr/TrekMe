package com.peterlaurence.trekme.core.lib.gpx.model


data class Metadata(val name: String? = null, val time: Long? = null, val bounds: Bounds? = null,
                    val elevationSourceInfo: GpxElevationSourceInfo? = null)

/**
 * Defines an area. Conform to the GPX 1.1 specification.
 */
data class Bounds(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double)

/**
 * Container for elevation source's properties.
 * The [sampling] indicates the rate at which elevation is subjects to change. For example, when
 * all points can potentially has different elevation values, the sampling is 1.
 */
data class GpxElevationSourceInfo(val elevationSource: GpxElevationSource, val sampling: Int)

enum class GpxElevationSource {
    GPS, IGN_RGE_ALTI, UNKNOWN
}