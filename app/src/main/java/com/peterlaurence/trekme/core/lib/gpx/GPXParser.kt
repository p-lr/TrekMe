package com.peterlaurence.trekme.core.lib.gpx

import android.util.Xml
import com.peterlaurence.trekme.core.lib.gpx.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.collections.ArrayList


/**
 * A GPX parser compliant with the [GPX 1.1 schema](https://www.topografix.com/gpx/1/1/)
 *
 * @author P.Laurence on 12/02/17.
 */
@Throws(XmlPullParserException::class, IOException::class, ParseException::class)
suspend fun parseGpx(`in`: InputStream): Gpx = withContext(Dispatchers.IO) {
    `in`.use {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(it, null)
        parser.nextTag()
        readGpx(parser)
    }
}

/**
 * A variant of [parseGpx] which returns a [Gpx] instance or null if case of failure.
 */
suspend fun parseGpxSafely(input: InputStream): Gpx? = withContext(Dispatchers.IO) {
    runCatching {
        input.use {
            parseGpx(it)
        }
    }.getOrNull()
}

@Throws(XmlPullParserException::class, IOException::class, ParseException::class)
private fun readGpx(parser: XmlPullParser): Gpx {
    var metadata: Metadata? = null
    val tracks = ArrayList<Track>()
    val wayPoints = ArrayList<TrackPoint>()
    parser.require(XmlPullParser.START_TAG, null, TAG_GPX)
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }
        // Starts by looking for the entry tag
        when (parser.name) {
            TAG_METADATA -> metadata = readMetadata(parser)
            TAG_TRACK -> tracks.add(readTrack(parser))
            TAG_WAYPOINT -> wayPoints.add(readPoint(parser, tag = TAG_WAYPOINT))
            TAG_ROUTE -> tracks.add(readRoute(parser))
            else -> skip(parser)
        }
    }
    parser.require(XmlPullParser.END_TAG, null, TAG_GPX)
    return Gpx(tracks = tracks, wayPoints = wayPoints, metadata = metadata)
}

private fun readMetadata(parser: XmlPullParser): Metadata {
    var name: String? = null
    var time: Long? = null
    var eleSrcInfo: GpxElevationSourceInfo? = null
    parser.require(XmlPullParser.START_TAG, null, TAG_METADATA)
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }
        when (parser.name) {
            TAG_NAME -> name = readName(parser)
            TAG_TIME -> time = readTime(parser)
            TAG_ELE_SOURCE_INFO -> eleSrcInfo = readEleSourceInfo(parser)
            else -> skip(parser)
        }
    }
    parser.require(XmlPullParser.END_TAG, null, TAG_METADATA)
    return Metadata(name, time, elevationSourceInfo = eleSrcInfo)
}

@Throws(IOException::class, XmlPullParserException::class, ParseException::class)
private fun readEleSourceInfo(parser: XmlPullParser): GpxElevationSourceInfo? {
    return try {
        parser.require(XmlPullParser.START_TAG, null, TAG_ELE_SOURCE_INFO)
        val source = parser.getAttributeValue(null, ATTR_ELE_SOURCE).toString()
        val sampling = parser.getAttributeValue(null, ATTR_SAMPLING).toInt()
        val eleSrc = GpxElevationSource.values().firstOrNull { it.toString() == source }
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
        }
        parser.require(XmlPullParser.END_TAG, null, TAG_ELE_SOURCE_INFO)
        eleSrc?.let { GpxElevationSourceInfo(eleSrc, sampling) }
    } catch (e: Exception) {
        null
    }
}

/**
 * Parses the contents of a route, which interpreted as a track with a single segment.
 */
@Throws(XmlPullParserException::class, IOException::class, ParseException::class)
private fun readRoute(parser: XmlPullParser): Track {
    val segments = ArrayList<TrackSegment>()
    val points = ArrayList<TrackPoint>()
    parser.require(XmlPullParser.START_TAG, null, TAG_ROUTE)
    var trackName = ""
    var trkExtensions: TrackExtensions? = null
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }
        when (parser.name) {
            TAG_NAME -> trackName = readName(parser)
            TAG_RTE_POINT -> points.add(readPoint(parser, tag = TAG_RTE_POINT))
            TAG_EXTENSIONS -> trkExtensions = readTrackExtensions(parser)
            else -> skip(parser)
        }
    }
    parser.require(XmlPullParser.END_TAG, null, TAG_ROUTE)

    segments.add(TrackSegment(points))
    return Track(trackSegments = segments, name = trackName, id = trkExtensions?.id)
}

/**
 * Parses the contents of a track.
 *
 * If it encounters a title, summary, or link tag, hands them off to their respective "read"
 * methods for processing. Otherwise, skips the tag.
 */
@Throws(XmlPullParserException::class, IOException::class, ParseException::class)
private fun readTrack(parser: XmlPullParser): Track {
    val segments = ArrayList<TrackSegment>()
    parser.require(XmlPullParser.START_TAG, null, TAG_TRACK)
    var trackName = ""
    var trkExtensions: TrackExtensions? = null
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }
        when (parser.name) {
            TAG_NAME -> trackName = readName(parser)
            TAG_SEGMENT -> segments.add(readSegment(parser))
            TAG_EXTENSIONS -> trkExtensions = readTrackExtensions(parser)
            else -> skip(parser)
        }
    }
    parser.require(XmlPullParser.END_TAG, null, TAG_TRACK)

    return Track(trackSegments = segments, name = trackName, id = trkExtensions?.id)
}

@Throws(IOException::class, XmlPullParserException::class, ParseException::class)
private fun readTrackExtensions(parser: XmlPullParser): TrackExtensions {
    parser.require(XmlPullParser.START_TAG, null, TAG_EXTENSIONS)
    var trackId: String? = null
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }
        when (parser.name) {
            TAG_TRK_ID -> trackId = readText(parser)
            else -> skip(parser)
        }
    }
    parser.require(XmlPullParser.END_TAG, null, TAG_EXTENSIONS)
    return TrackExtensions(trackId)
}

/* Process summary tags in the feed */
@Throws(IOException::class, XmlPullParserException::class, ParseException::class)
private fun readSegment(parser: XmlPullParser): TrackSegment {
    val points = ArrayList<TrackPoint>()
    parser.require(XmlPullParser.START_TAG, null, TAG_SEGMENT)
    var trkExtensions: TrackExtensions? = null
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }
        when (parser.name) {
            TAG_TRK_POINT -> points.add(readPoint(parser))
            TAG_EXTENSIONS -> trkExtensions = readTrackExtensions(parser)
            else -> skip(parser)
        }
    }
    parser.require(XmlPullParser.END_TAG, null, TAG_SEGMENT)
    return TrackSegment(points, id = trkExtensions?.id)
}

/* Process summary tags in the feed */
@Throws(IOException::class, XmlPullParserException::class, ParseException::class)
private fun readPoint(parser: XmlPullParser, tag: String = TAG_TRK_POINT): TrackPoint {
    val trackPoint = TrackPoint()

    parser.require(XmlPullParser.START_TAG, null, tag)
    trackPoint.latitude = java.lang.Double.valueOf(parser.getAttributeValue(null, ATTR_LAT))
    trackPoint.longitude = java.lang.Double.valueOf(parser.getAttributeValue(null, ATTR_LON))
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }
        when (parser.name) {
            TAG_ELEVATION -> trackPoint.elevation = readElevation(parser)
            TAG_TIME -> trackPoint.time = readTime(parser)
            TAG_NAME -> trackPoint.name = readName(parser)
            else -> skip(parser)
        }
    }
    parser.require(XmlPullParser.END_TAG, null, tag)
    return trackPoint
}

@Throws(IOException::class, XmlPullParserException::class)
private fun readName(parser: XmlPullParser): String {
    parser.require(XmlPullParser.START_TAG, null, TAG_NAME)
    val name = readText(parser)
    parser.require(XmlPullParser.END_TAG, null, TAG_NAME)
    return name
}

@Throws(IOException::class, XmlPullParserException::class)
private fun readElevation(parser: XmlPullParser): Double? {
    parser.require(XmlPullParser.START_TAG, null, TAG_ELEVATION)
    val ele = java.lang.Double.valueOf(readText(parser))
    parser.require(XmlPullParser.END_TAG, null, TAG_ELEVATION)
    return ele
}

@Throws(IOException::class, XmlPullParserException::class, ParseException::class)
private fun readTime(parser: XmlPullParser): Long? {
    return try {
        parser.require(XmlPullParser.START_TAG, null, TAG_TIME)
        val time = parseIsoDate(readText(parser))
        parser.require(XmlPullParser.END_TAG, null, TAG_TIME)
        time
    } catch (e: Exception) {
        null
    }
}

@Throws(IOException::class, XmlPullParserException::class)
private fun readText(parser: XmlPullParser): String {
    var result = ""
    if (parser.next() == XmlPullParser.TEXT) {
        result = parser.text
        parser.nextTag()
    }
    return result
}

@Throws(XmlPullParserException::class, IOException::class)
private fun skip(parser: XmlPullParser) {
    if (parser.eventType != XmlPullParser.START_TAG) {
        throw IllegalStateException()
    }
    var depth = 1
    while (depth != 0) {
        when (parser.next()) {
            XmlPullParser.END_TAG -> depth--
            XmlPullParser.START_TAG -> depth++
        }
    }
}

private fun parseIsoDate(dateStr: String): Long {
    return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC).toEpochMilli()
}

private data class TrackExtensions(val id: String?)