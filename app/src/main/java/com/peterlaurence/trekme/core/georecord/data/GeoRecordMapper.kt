package com.peterlaurence.trekme.core.georecord.data

import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.lib.gpx.model.*
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route

/**
 * Converts a [Gpx] instance into view-specific types.
 * Theoretically, this method should return a list of [GeoRecord], as a track may contain several
 * segments, each of which map to a [Route].
 * Should be invoked off UI thread.
 */
fun convertGpx(
    gpx: Gpx,
    defaultName: String = "track"
): GeoRecord {
    val routes = gpx.tracks.mapIndexed { index, track ->
        gpxTrackToRoute(track, gpx.hasTrustedElevations(), index, defaultName)
    }.flatten()
    val waypoints = gpx.wayPoints.mapIndexed { index, wpt ->
        gpxWaypointToMarker(wpt, index, defaultName)
    }
    return GeoRecord(routes, waypoints, gpx.metadata?.time, gpx.hasTrustedElevations())
}

/**
 * Converts a [Track] into a list of [Route] (a single [Track] may contain several [TrackSegment]).
 * Should be invoked off UI thread.
 */
fun gpxTrackToRoute(
    track: Track,
    elevationTrusted: Boolean,
    index: Int,
    defaultName: String
): List<Route> {

    /* The route name is the track name if it has one. Otherwise we take the default name */
    val name = track.name.ifEmpty {
        "$defaultName#$index"
    }

    /* If there's more than one segment, the route name/id is the track name/id suffixed
     * with the segment index. */
    fun String?.formatNameOrId(i: Int): String? = if (this != null && track.trackSegments.size > 1) {
        this + "_$i"
    } else this

    /* Make a route for each track segment */
    return track.trackSegments.mapIndexed { i, segment ->
        val markers = segment.trackPoints.map { trackPoint ->
            trackPoint.toMarker()
        }.toMutableList()

        Route(
            id = segment.id,
            name = name.formatNameOrId(i),
            initialMarkers = markers,
            initialVisibility = true, /* The route should be visible by default */
            elevationTrusted = elevationTrusted
        )
    }
}

fun gpxWaypointToMarker(
    wpt: TrackPoint,
    index: Int,
    defaultName: String
): Marker {
    return wpt.toMarker().apply {
        name = if (wpt.name?.isNotEmpty() == true) {
            wpt.name ?: ""
        } else {
            "$defaultName-wpt${index + 1}"
        }
    }
}

fun TrackPoint.toMarker(): Marker = Marker(lat = latitude, lon = longitude, elevation = elevation, time = time)