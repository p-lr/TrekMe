package com.peterlaurence.trekadvisor.util.gpxparser;

import android.util.Xml;

import com.peterlaurence.trekadvisor.util.gpxparser.model.Gpx;
import com.peterlaurence.trekadvisor.util.gpxparser.model.Track;
import com.peterlaurence.trekadvisor.util.gpxparser.model.TrackPoint;
import com.peterlaurence.trekadvisor.util.gpxparser.model.TrackSegment;

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


/**
 * A GPX parser.
 *
 * @author peterLaurence on 12/02/17.
 */
public class GPXParser {

    private static final String TAG_GPX = "gpx";
    private static final String TAG_TRACK = "trk";
    private static final String TAG_SEGMENT = "trkseg";
    private static final String TAG_POINT = "trkpt";
    private static final String TAG_LAT = "lat";
    private static final String TAG_LON = "lon";
    private static final String TAG_ELEVATION = "ele";
    private static final String TAG_TIME = "time";

    static private final String ns = null;

    private static final DateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


    public Gpx parse(InputStream in) throws XmlPullParserException, IOException, ParseException {
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

    private Gpx readGpx(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
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
        return new Gpx.Builder()
                .setTracks(tracks)
                .build();
    }

    /**
     * Parses the contents of an entry. <p>
     *
     * If it encounters a title, summary, or link tag, hands them off to their respective "read"
     * methods for processing. Otherwise, skips the tag.
     */
    private Track readTrack(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        List<TrackSegment> segments = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, ns, TAG_TRACK);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_SEGMENT:
                    segments.add(readSegment(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_TRACK);
        return new Track.Builder()
                .setTrackSegments(segments)
                .build();
    }

    /* Process summary tags in the feed */
    private TrackSegment readSegment(XmlPullParser parser) throws IOException, XmlPullParserException, ParseException {
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
        return new TrackSegment.Builder()
                .setTrackPoints(points)
                .build();
    }

    /* Process summary tags in the feed */
    private TrackPoint readPoint(XmlPullParser parser) throws IOException, XmlPullParserException, ParseException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_POINT);
        Double lat = Double.valueOf(parser.getAttributeValue(null, TAG_LAT));
        Double lng = Double.valueOf(parser.getAttributeValue(null, TAG_LON));
        Double ele = null;
        Date time = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_ELEVATION:
                    ele = readElevation(parser);
                    break;
                case TAG_TIME:
                    time = readTime(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_POINT);
        return new TrackPoint.Builder()
                .setElevation(ele)
                .setLatitude(lat)
                .setLongitude(lng)
                .setTime(time)
                .build();
    }

    private Double readElevation(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_ELEVATION);
        Double ele = Double.valueOf(readText(parser));
        parser.require(XmlPullParser.END_TAG, ns, TAG_ELEVATION);
        return ele;
    }

    private Date readTime(XmlPullParser parser) throws IOException, XmlPullParserException, ParseException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_TIME);
        Date time = DATE_PARSER.parse(readText(parser));
        parser.require(XmlPullParser.END_TAG, ns, TAG_TIME);
        return time;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
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