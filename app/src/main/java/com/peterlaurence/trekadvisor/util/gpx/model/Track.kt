package com.peterlaurence.trekadvisor.util.gpx.model

/**
 * Represents a track - an ordered list of Track Segment describing a path.
 *
 * @author peterLaurence on 12/02/17.
 */
class Track(
        val trackSegments: List<TrackSegment>,
        val name: String = ""
)
