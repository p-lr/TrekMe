package com.peterlaurence.trekme.util.gpx.model

/**
 * A Track Segment holds a list of Track Points which are logically connected in order.
 *
 * @author P.Laurence on 12/02/17.
 */
data class TrackSegment(val trackPoints: List<TrackPoint>, val id: String? = null)

