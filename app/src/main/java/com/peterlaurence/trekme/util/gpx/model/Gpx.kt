package com.peterlaurence.trekme.util.gpx.model

/**
 * GPX documents have a version and a creator as attributes, and contains an optional metadata header,
 * followed by waypoints, routes, and tracks.
 *
 * Custom elements can be added to the extensions section of the GPX document.
 *
 * @author P.Laurence on 12/02/17.
 */
data class Gpx(
        val metadata: Metadata? = null,
        val tracks: List<Track>,
        val wayPoints: List<TrackPoint>,
        val creator: String = "",
        var version: String = "1.1"
)

/**
 * For instance, only trust [ElevationSource.IGN_RGE_ALTI].
 */
fun Gpx.hasTrustedElevations(): Boolean {
    return metadata?.elevationSourceInfo?.elevationSource == ElevationSource.IGN_RGE_ALTI
}

fun Gpx.getElevationSource(): ElevationSource {
    return metadata?.elevationSourceInfo?.elevationSource ?: ElevationSource.UNKNOWN
}
