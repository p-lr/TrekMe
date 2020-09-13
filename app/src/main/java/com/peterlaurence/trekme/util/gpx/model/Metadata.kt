package com.peterlaurence.trekme.util.gpx.model


data class Metadata(val name: String? = null, val time: Long? = null, val bounds: Bounds? = null)

/**
 * Defines an area. Conform to the GPX 1.1 specification.
 */
data class Bounds(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double)