package com.peterlaurence.trekadvisor.util.gpx

import android.util.Xml
import com.peterlaurence.trekadvisor.util.gpx.model.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


/**
 * A GPX parser.
 *
 * @author peterLaurence on 12/02/17.
 */
object GPXParser {
    private val ns: String? = null

    private val DATE_PARSER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)


    @Throws(XmlPullParserException::class, IOException::class, ParseException::class)
    fun parse(`in`: InputStream): Gpx {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(`in`, null)
            parser.nextTag()
            return readGpx(parser)
        } finally {
            `in`.close()
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
     *
     *
     * If it encounters a title, summary, or link tag, hands them off to their respective "read"
     * methods for processing. Otherwise, skips the tag.
     */
    @Throws(XmlPullParserException::class, IOException::class, ParseException::class)
    private fun readTrack(parser: XmlPullParser): Track {
        val segments = ArrayList<TrackSegment>()
        parser.require(XmlPullParser.START_TAG, ns, TAG_TRACK)
        var trackName = ""
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            when (name) {
                TAG_NAME -> trackName = readName(parser)
                TAG_SEGMENT -> segments.add(readSegment(parser))
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_TRACK)

        return Track(trackSegments = segments, name = trackName)
    }

    /* Process summary tags in the feed */
    @Throws(IOException::class, XmlPullParserException::class, ParseException::class)
    private fun readSegment(parser: XmlPullParser): TrackSegment {
        val builder = TrackSegment.Builder()

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
        return builder.setTrackPoints(points)
                .build()
    }

    /* Process summary tags in the feed */
    @Throws(IOException::class, XmlPullParserException::class, ParseException::class)
    private fun readPoint(parser: XmlPullParser): TrackPoint {
        val builder = TrackPoint.Builder()

        parser.require(XmlPullParser.START_TAG, ns, TAG_POINT)
        builder.setLatitude(java.lang.Double.valueOf(parser.getAttributeValue(null, ATTR_LAT)))
        builder.setLongitude(java.lang.Double.valueOf(parser.getAttributeValue(null, ATTR_LON)))
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            when (name) {
                TAG_ELEVATION -> builder.setElevation(readElevation(parser))
                TAG_TIME -> builder.setTime(readTime(parser))
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_POINT)
        return builder.build()
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
    private fun readTime(parser: XmlPullParser): Date {
        parser.require(XmlPullParser.START_TAG, ns, TAG_TIME)
        val time = DATE_PARSER.parse(readText(parser))
        parser.require(XmlPullParser.END_TAG, ns, TAG_TIME)
        return time
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
}