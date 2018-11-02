package com.peterlaurence.trekadvisor.util.gpx

import android.util.Xml
import com.peterlaurence.trekadvisor.core.track.TrackStatistics
import com.peterlaurence.trekadvisor.util.gpx.model.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


/**
 * A GPX parser compliant with the [GPX 1.1 schema](https://www.topografix.com/gpx/1/1/)
 *
 * @author peterLaurence on 12/02/17.
 */
object GPXParser {
    private val ns: String? = null

    private val DATE_PARSER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)


    @Throws(XmlPullParserException::class, IOException::class, ParseException::class)
    fun parse(`in`: InputStream): Gpx {
        `in`.use {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(it, null)
            parser.nextTag()
            return readGpx(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class, ParseException::class)
    private fun readGpx(parser: XmlPullParser): Gpx {
        val tracks = ArrayList<Track>()
        parser.require(XmlPullParser.START_TAG, ns, TAG_GPX)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            // Starts by looking for the entry tag
            when (name) {
                TAG_TRACK -> tracks.add(readTrack(parser))
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_GPX)
        return Gpx(tracks = tracks)
    }

    /**
     * Parses the contents of an entry.
     *
     * If it encounters a title, summary, or link tag, hands them off to their respective "read"
     * methods for processing. Otherwise, skips the tag.
     */
    @Throws(XmlPullParserException::class, IOException::class, ParseException::class)
    private fun readTrack(parser: XmlPullParser): Track {
        val segments = ArrayList<TrackSegment>()
        parser.require(XmlPullParser.START_TAG, ns, TAG_TRACK)
        var trackName = ""
        var trackStatistics: TrackStatistics? = null
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            when (name) {
                TAG_NAME -> trackName = readName(parser)
                TAG_SEGMENT -> segments.add(readSegment(parser))
                TAG_EXTENSIONS -> trackStatistics = readTrackExtensions(parser)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_TRACK)

        return Track(trackSegments = segments, name = trackName, statistics = trackStatistics)
    }

    @Throws(IOException::class, XmlPullParserException::class, ParseException::class)
    private fun readTrackExtensions(parser: XmlPullParser): TrackStatistics? {
        parser.require(XmlPullParser.START_TAG, ns, TAG_EXTENSIONS)
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
        parser.require(XmlPullParser.END_TAG, ns, TAG_EXTENSIONS)
        return trackStatistics
    }

    @Throws(IOException::class, XmlPullParserException::class, ParseException::class)
    private fun readTrackStatistics(parser: XmlPullParser): TrackStatistics {
        parser.require(XmlPullParser.START_TAG, ns, TAG_TRACK_STATISTICS)
        val trackStatistics = TrackStatistics(0.0, 0.0, 0.0, 0.0, 0)
        trackStatistics.distance = parser.getAttributeValue(null, ATTR_TRK_STAT_DIST)?.toDouble() ?: 0.0
        trackStatistics.elevationDifferenceMax = parser.getAttributeValue(null, ATTR_TRK_STAT_ELE_DIFF_MAX)?.toDouble() ?: 0.0
        trackStatistics.elevationUpStack = parser.getAttributeValue(null, ATTR_TRK_STAT_ELE_UP_STACK)?.toDouble() ?: 0.0
        trackStatistics.elevationDownStack = parser.getAttributeValue(null, ATTR_TRK_STAT_ELE_DOWN_STACK)?.toDouble() ?: 0.0
        trackStatistics.durationInSecond = parser.getAttributeValue(null, ATTR_TRK_STAT_DURATION)?.toLong() ?: 0
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            skip(parser)
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_TRACK_STATISTICS)
        return trackStatistics
    }

    /* Process summary tags in the feed */
    @Throws(IOException::class, XmlPullParserException::class, ParseException::class)
    private fun readSegment(parser: XmlPullParser): TrackSegment {
        val points = ArrayList<TrackPoint>()
        parser.require(XmlPullParser.START_TAG, ns, TAG_SEGMENT)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            when (name) {
                TAG_POINT -> points.add(readPoint(parser))
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_SEGMENT)
        return TrackSegment(points)
    }

    /* Process summary tags in the feed */
    @Throws(IOException::class, XmlPullParserException::class, ParseException::class)
    private fun readPoint(parser: XmlPullParser): TrackPoint {
        val trackPoint = TrackPoint()

        parser.require(XmlPullParser.START_TAG, ns, TAG_POINT)
        trackPoint.latitude = java.lang.Double.valueOf(parser.getAttributeValue(null, ATTR_LAT))
        trackPoint.longitude = java.lang.Double.valueOf(parser.getAttributeValue(null, ATTR_LON))
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            when (name) {
                TAG_ELEVATION -> trackPoint.elevation = readElevation(parser)
                TAG_TIME -> trackPoint.time = readTime(parser)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_POINT)
        return trackPoint
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readName(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, TAG_NAME)
        val name = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, TAG_NAME)
        return name
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readElevation(parser: XmlPullParser): Double? {
        parser.require(XmlPullParser.START_TAG, ns, TAG_ELEVATION)
        val ele = java.lang.Double.valueOf(readText(parser))
        parser.require(XmlPullParser.END_TAG, ns, TAG_ELEVATION)
        return ele
    }

    @Throws(IOException::class, XmlPullParserException::class, ParseException::class)
    private fun readTime(parser: XmlPullParser): Long? {
        return try {
            parser.require(XmlPullParser.START_TAG, ns, TAG_TIME)
            val time = DATE_PARSER.parse(readText(parser))
            parser.require(XmlPullParser.END_TAG, ns, TAG_TIME)
            time.time
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
    fun getDateParser(): SimpleDateFormat {
        return DATE_PARSER
    }
}