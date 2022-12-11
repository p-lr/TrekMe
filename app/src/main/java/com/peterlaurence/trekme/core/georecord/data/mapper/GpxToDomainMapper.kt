package com.peterlaurence.trekme.core.georecord.data.mapper

import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.RouteGroup
import com.peterlaurence.trekme.core.lib.gpx.model.*
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.features.common.domain.interactors.georecord.hasTrustedElevations
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource
import com.peterlaurence.trekme.features.common.domain.model.ElevationSourceInfo
import java.util.*

/**
 * Converts a [Gpx] instance into view-specific types.
 * Should be invoked off UI thread.
 */
fun gpxToDomain(
    gpx: Gpx,
    name: String? = null
): GeoRecord {
    val eleSourceInfo = gpx.metadata?.elevationSourceInfo?.let {
        gpxEleSourceInfoToDomain(it)
    }

    val routeGroups = gpx.tracks.mapIndexed { index, track ->
        gpxTrackToRoute(track, eleSourceInfo.hasTrustedElevations(), index, name ?: "track")
    }

    val waypoints = gpx.wayPoints.mapIndexed { index, wpt ->
        gpxWaypointToMarker(wpt, index)
    }

    return GeoRecord(
        UUID.randomUUID(),
        routeGroups,
        waypoints,
        gpx.metadata?.time,
        eleSourceInfo,
        name ?: gpx.metadata?.name ?: "recording"
    )
}

/**
 * Converts a [Track] into a [RouteGroup] (a single [Track] may contain several [TrackSegment], and
 * each [TrackSegment] corresponds to a [Route]).
 * Should be invoked off UI thread.
 */
private fun gpxTrackToRoute(
    track: Track,
    elevationTrusted: Boolean,
    index: Int,
    defaultName: String
): RouteGroup {

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
    }.let {
        RouteGroup(track.id ?: UUID.randomUUID().toString(), routes = it, name = track.name)
    }
}

private fun gpxWaypointToMarker(
    wpt: TrackPoint,
    index: Int,
): Marker {
    return wpt.toMarker(
        name = if (wpt.name?.isNotEmpty() == true) {
            wpt.name ?: ""
        } else {
            "wpt-${index + 1}"
        }
    )
}

private fun gpxEleSourceInfoToDomain(gpxElevationSourceInfo: GpxElevationSourceInfo): ElevationSourceInfo {
    val elevationSource = when(gpxElevationSourceInfo.elevationSource) {
        GpxElevationSource.GPS -> ElevationSource.GPS
        GpxElevationSource.IGN_RGE_ALTI -> ElevationSource.IGN_RGE_ALTI
        GpxElevationSource.UNKNOWN -> ElevationSource.UNKNOWN
    }

    return ElevationSourceInfo(elevationSource, gpxElevationSourceInfo.sampling)
}

fun TrackPoint.toMarker(name: String = ""): Marker = Marker(
    lat = latitude,
    lon = longitude,
    elevation = elevation,
    time = time,
    name = name
)