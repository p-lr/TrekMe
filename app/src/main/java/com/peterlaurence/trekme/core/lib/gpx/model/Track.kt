package com.peterlaurence.trekme.core.lib.gpx.model

import com.peterlaurence.trekme.core.track.TrackStatistics

/**
 * Represents a track - an ordered list of Track Segment describing a path.
 *
 * @author P.Laurence on 12/02/17.
 */
data class Track @JvmOverloads constructor(
        val trackSegments: List<TrackSegment>,
        val name: String = "",
        val id: String? = null, // introduced in 2.7.7, uniquely identifies a track
        var statistics: TrackStatistics? = null
)
