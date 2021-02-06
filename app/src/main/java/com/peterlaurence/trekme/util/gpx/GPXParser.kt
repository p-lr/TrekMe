package com.peterlaurence.trekme.util.gpx

import android.util.Xml
import com.peterlaurence.trekme.core.track.TrackStatistics
import com.peterlaurence.trekme.util.gpx.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.getOrSet


/**
 * A GPX parser compliant with the [GPX 1.1 schema](https://www.topografix.com/gpx/1/1/)
 *
 * @author P.Laurence on 12/02/17.
 */
@Suppress("BlockingMethodInNonBlockingContext")
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
    var eleSrcInfo: ElevationSourceInfo? = null
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
private fun readEleSourceInfo(parser: XmlPullParser): ElevationSourceInfo? {
    return try {
        parser.require(XmlPullParser.START_TAG, null, TAG_ELE_SOURCE_INFO)
        val source = parser.getAttributeValue(null, ATTR_ELE_SOURCE).toString()
        val sampling = parser.getAttributeValue(null, ATTR_SAMPLING).toInt()
        val eleSrc = ElevationSource.values().firstOrNull { it.toString() == source }
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
        }
        parser.require(XmlPullParser.END_TAG, null, TAG_ELE_SOURCE_INFO)
        eleSrc?.let { ElevationSourceInfo(eleSrc, sampling) }
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
    var trackStatistics: TrackStatistics? = null
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }
        when (parser.name) {
            TAG_NAME -> trackName = readName(parser)
            TAG_RTE_POINT -> points.add(readPoint(parser, tag = TAG_RTE_POINT))
            TAG_EXTENSIONS -> trackStatistics = readTrackExtensions(parser)
            else -> skip(parser)
        }
    }
    parser.require(XmlPullParser.END_TAG, null, TAG_ROUTE)

    segments.add(TrackSegment(points))
    return Track(trackSegments = segments, name = trackName, statistics = trackStatistics)
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
    var trackStatistics: TrackStatistics? = null
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }
        when (parser.name) {
            TAG_NAME -> trackName = readName(parser)
            TAG_SEGMENT -> segments.add(readSegment(parser))
            TAG_EXTENSIONS -> trackStatistics = readTrackExtensions(parser)
            else -> skip(parser)
        }
    }
    parser.require(XmlPullParser.END_TAG, null, TAG_TRACK)

    return Track(trackSegments = segments, name = trackName, statistics = trackStatistics)
}

@Throws(IOException::class, XmlPullParserException::class, ParseException::class)
private fun readTrackExtensions(parser: XmlPullParser): TrackStatistics? {
    parser.require(XmlPullParser.START_TAG, null, TAG_EXTENSIONS)
    var trackStatistics: TrackStatistics? = null
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }
        when (parser.name) {
            TAG_TRACK_STATISTICS -> trackStatistics = readTrackStatistics(parser)
            else -> skip(parser)
        }
    }
    parser.require(XmlPullParser.END_TAG, null, TAG_EXTENSIONS)
    return trackStatistics
}

@Throws(IOException::class, XmlPullParserException::class, ParseException::class)
private fun readTrackStatistics(parser: XmlPullParser): TrackStatistics {
    parser.require(XmlPullParser.START_TAG, null, TAG_TRACK_STATISTICS)

    val distance = runCatching {
        parser.getAttributeValue(null, ATTR_TRK_STAT_DIST)?.toDouble()
    }.getOrNull() ?: 0.0

    val elevationDifferenceMax = runCatching {
        parser.getAttributeValue(null, ATTR_TRK_STAT_ELE_DIFF_MAX)?.toDouble()
    }.getOrNull() ?: 0.0

    val elevationUpStack = runCatching {
        parser.getAttributeValue(null, ATTR_TRK_STAT_ELE_UP_STACK)?.toDouble()
    }.getOrNull() ?: 0.0

    val elevationDownStack = runCatching {
        parser.getAttributeValue(null, ATTR_TRK_STAT_ELE_DOWN_STACK)?.toDouble()
    }.getOrNull() ?: 0.0

    val durationInSecond = runCatching {
        parser.getAttributeValue(null, ATTR_TRK_STAT_DURATION)?.toLong()
    }.getOrNull()

    val avgSpeed = runCatching {
        parser.getAttributeValue(null, ATTR_TRK_STAT_AVG_SPEED)?.toDouble()
    }.getOrNull()

    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }
        skip(parser)
    }
    parser.require(XmlPullParser.END_TAG, null, TAG_TRACK_STATISTICS)
    return TrackStatistics(distance, elevationDifferenceMax, elevationUpStack, elevationDownStack, durationInSecond, avgSpeed)
}

/* Process summary tags in the feed */
@Throws(IOException::class, XmlPullParserException::class, ParseException::class)
private fun readSegment(parser: XmlPullParser): TrackSegment {
    val points = ArrayList<TrackPoint>()
    parser.require(XmlPullParser.START_TAG, null, TAG_SEGMENT)
    while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            continue
        }
        when (parser.name) {
            TAG_TRK_POINT -> points.add(readPoint(parser))
            else -> skip(parser)
        }
    }
    parser.require(XmlPullParser.END_TAG, null, TAG_SEGMENT)
    return TrackSegment(points)
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
        val time = DATE_PARSER.parse(readText(parser))
        parser.require(XmlPullParser.END_TAG, null, TAG_TIME)
        time?.time
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

/**
 * For unit test purposes
 */
fun getGpxDateParser(): SimpleDateFormat {
    return DATE_PARSER
}

private val DATE_PARSER_tl = ThreadLocal<SimpleDateFormat>()

/**
 * We use a [ThreadLocal] because [SimpleDateFormat] isn't thread-safe, and the gpx parser might be
 * invoked from multiple threads. Therefore, we cannot use a unique instance of [SimpleDateFormat].
 *
 * We don't add the trailing 'Z', because sometimes it's missing and we don't care about a
 * better precision than the second.
 **/
private val DATE_PARSER: SimpleDateFormat
    get() = DATE_PARSER_tl.getOrSet {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
    }