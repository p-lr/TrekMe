package com.peterlaurence.trekme.core.georecord.data.mapper

import com.peterlaurence.trekme.core.appName
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.RouteGroup
import com.peterlaurence.trekme.core.lib.gpx.model.*
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource
import com.peterlaurence.trekme.features.common.domain.model.ElevationSourceInfo

fun GeoRecord.toGpx(): Gpx {
    val elevationSourceInfo = elevationSourceInfo?.toData()
    /* We don't need to save bounds in this context. It's only useful when a recording is made
     * within the app and needs to be automatically imported. */
    val metadata = Metadata(name, time, elevationSourceInfo = elevationSourceInfo)

    val tracks = routeGroups.map { it.toTrack() }

    val wayPoints = markers.map { it.toTrackPoint() }
    val creator = appName

    return Gpx(metadata, tracks, wayPoints, creator)
}

private fun ElevationSourceInfo.toData(): GpxElevationSourceInfo {
    val elevationSource = when (this.elevationSource) {
        ElevationSource.GPS -> GpxElevationSource.GPS
        ElevationSource.IGN_RGE_ALTI -> GpxElevationSource.IGN_RGE_ALTI
        ElevationSource.UNKNOWN -> GpxElevationSource.UNKNOWN
    }

    return GpxElevationSourceInfo(elevationSource, sampling)
}

private fun RouteGroup.toTrack(): Track {
    return Track(id = id, name = name, trackSegments = routes.map { it.toTrackSegment() })
}

private fun Route.toTrackSegment(): TrackSegment {
    return TrackSegment(id = id, trackPoints = routeMarkers.map { it.toTrackPoint() })
}

private fun Marker.toTrackPoint(): TrackPoint {
    return TrackPoint(lat, lon, elevation = elevation, time = time, name = name)
}