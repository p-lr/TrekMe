package com.peterlaurence.trekadvisor.util.gpx;

import android.util.Xml;

import com.peterlaurence.trekadvisor.util.gpx.model.Gpx;
import com.peterlaurence.trekadvisor.util.gpx.model.Track;
import com.peterlaurence.trekadvisor.util.gpx.model.TrackPoint;
import com.peterlaurence.trekadvisor.util.gpx.model.TrackSegment;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.peterlaurence.trekadvisor.util.gpx.model.GpxSchema.*;


/**
 * A GPX parser.
 *
 * @author peterLaurence on 12/02/17.
 */
public abstract class GPXParser {
    private static final String ns = null;

    private static final DateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);


    public static Gpx parse(InputStream in) throws XmlPullParserException, IOException, ParseException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(in, null);
            parser.nextTag();
            return readGpx(parser);
        } finally {
            in.close();
        }
    }

    private static Gpx readGpx(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        Gpx.Builder builder = new Gpx.Builder();

        List<Track> tracks = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, ns, TAG_GPX);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            switch (name) {
                case TAG_TRACK:
                    tracks.add(readTrack(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_GPX);
        return builder.setTracks(tracks)
                .build();
    }

    /**
     * Parses the contents of an entry. <p>
     *
     * If it encounters a title, summary, or link tag, hands them off to their respective "read"
     * methods for processing. Otherwise, skips the tag.
     */
    private static Track readTrack(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        Track.Builder builder = new Track.Builder();

        List<TrackSegment> segments = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, ns, TAG_TRACK);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_NAME:
                    builder.setName(readName(parser));
                    break;
                case TAG_SEGMENT:
                    segments.add(readSegment(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_TRACK);

        return builder.setTrackSegments(segments)
                .build();
    }

    /* Process summary tags in the feed */
    private static TrackSegment readSegment(XmlPullParser parser) throws IOException, XmlPullParserException, ParseException {
        TrackSegment.Builder builder = new TrackSegment.Builder();

        List<TrackPoint> points = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, ns, TAG_SEGMENT);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_POINT:
                    points.add(readPoint(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_SEGMENT);
        return builder.setTrackPoints(points)
                .build();
    }

    /* Process summary tags in the feed */
    private static TrackPoint readPoint(XmlPullParser parser) throws IOException, XmlPullParserException, ParseException {
        TrackPoint.Builder builder = new TrackPoint.Builder();

        parser.require(XmlPullParser.START_TAG, ns, TAG_POINT);
        builder.setLatitude(Double.valueOf(parser.getAttributeValue(null, ATTR_LAT)));
        builder.setLongitude(Double.valueOf(parser.getAttributeValue(null, ATTR_LON)));
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_ELEVATION:
                    builder.setElevation(readElevation(parser));
                    break;
                case TAG_TIME:
                    builder.setTime(readTime(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_POINT);
        return builder.build();
    }

    private static String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_NAME);
        String name = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, TAG_NAME);
        return name;
    }

    private static Double readElevation(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_ELEVATION);
        Double ele = Double.valueOf(readText(parser));
        parser.require(XmlPullParser.END_TAG, ns, TAG_ELEVATION);
        return ele;
    }

    private static Date readTime(XmlPullParser parser) throws IOException, XmlPullParserException, ParseException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_TIME);
        Date time = DATE_PARSER.parse(readText(parser));
        parser.require(XmlPullParser.END_TAG, ns, TAG_TIME);
        return time;
    }

    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}