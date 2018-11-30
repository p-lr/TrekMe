package com.peterlaurence.trekme.util.gpx.model

/**
 * GPX documents has a version and a creator as attributes, and contains a metadata header,
 * followed by waypoints, routes, and tracks.
 *
 *
 * Custom elements can be added to the extensions section of the GPX document.
 *
 * @author peterLaurence on 12/02/17.
 */
data class Gpx(
        val tracks: List<Track>,
        val wayPoints: List<TrackPoint>,
        val creator: String = "",
        var version: String = "1.1"
)
