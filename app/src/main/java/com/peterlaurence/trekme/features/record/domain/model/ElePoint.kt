package com.peterlaurence.trekme.features.record.domain.model

/**
 * A point representing the elevation at a given distance from the departure.
 *
 * @param dist distance in meters
 * @param elevation altitude in meters
 */
data class ElePoint(val dist: Double, val elevation: Double)
data class SegmentElePoints(val points: List<ElePoint>)