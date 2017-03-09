package com.peterlaurence.trekadvisor.core.track;

import android.os.AsyncTask;

import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;
import com.peterlaurence.trekadvisor.util.FileUtils;
import com.peterlaurence.trekadvisor.util.gpxparser.GPXParser;
import com.peterlaurence.trekadvisor.util.gpxparser.model.Gpx;
import com.peterlaurence.trekadvisor.util.gpxparser.model.Track;
import com.peterlaurence.trekadvisor.util.gpxparser.model.TrackPoint;
import com.peterlaurence.trekadvisor.util.gpxparser.model.TrackSegment;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * Manage operations related to track import.
 *
 * @author peterLaurence on 03/03/17.
 */
public class TrackImporter {
    private static final String[] supportedTrackFilesExtensions = new String[]{
            "gpx", "json", "xml"
    };

    public interface TrackFileParsedListener {
        void onTrackFileParsed(MapGson.Route route);

        void onError(String message);
    }

    /* Don't allow instantiation */
    private TrackImporter() {
    }

    public static boolean isFileSupported(File file) {
        String extension = FileUtils.getFileExtension(file);

        if ("".equals(extension)) return false;

        for (String ext : supportedTrackFilesExtensions) {
            if (ext.equals(extension)) return true;
        }
        return false;
    }

    /**
     * Parse a {@link File} that represents a routes, and is in one of the supported formats. <br>
     * The parsing is done in an asynctask.
     *
     * @param trackFiles a {@link File}
     * @param listener   a {@link TrackFileParsedListener}
     * @param map        the {@link Map} to which the routes will be added
     */
    public static void parseTrackFile(File[] trackFiles, TrackFileParsedListener listener, Map map) {
        GpxTrackFileTask gpxTrackFileTask = new GpxTrackFileTask(listener, map);
        gpxTrackFileTask.execute(trackFiles);
    }

    private static class GpxTrackFileTask extends AsyncTask<File, Void, MapGson.Route> {
        private TrackFileParsedListener mListener;
        private Map mMap;

        GpxTrackFileTask(TrackFileParsedListener listener, Map map) {
            mListener = listener;
            mMap = map;
        }

        /**
         * Each gpx file may contain several tracks. And each {@link Track} may contain several
         * {@link TrackSegment}. <br>
         * A {@link Track} is the equivalent of a {@link MapGson.Route}, so all {@link TrackSegment}
         * are added to a single {@link MapGson.Route}.
         */
        @Override
        protected MapGson.Route doInBackground(File... trackFiles) {
            GPXParser parser = new GPXParser();
            for (File file : trackFiles) {
                try {
                    Gpx gpx = parser.parse(new FileInputStream(file));

                    for (Track track : gpx.getTracks()) {
                        MapGson.Route route = toGsonTrack(track);
                        mMap.addRoute(route);
                    }
                } catch (XmlPullParserException | IOException | ParseException e) {
                    mListener.onError(e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(MapGson.Route route) {
            mListener.onTrackFileParsed(route);
        }

        /**
         * Converts a {@link Track} into a {@link MapGson.Route}. <br>
         * A single {@link Track} may contain several {@link TrackSegment}.
         */
        private MapGson.Route toGsonTrack(Track track) {
            /* Create a new route */
            MapGson.Route route = new MapGson.Route();

            List<TrackSegment> trackSegmentList = track.getTrackSegments();
            for (TrackSegment trackSegment : trackSegmentList) {
                List<TrackPoint> trackPointList = trackSegment.getTrackPoints();
                for (TrackPoint trackPoint : trackPointList) {
                    MapGson.Marker marker = new MapGson.Marker();

                    /* Here, the projected values obtained may be just the untouched latitude and
                     * longitude if the map doesn't use a projection. In both cases, we treat the
                     * data the same way : they are respectively stored as "proj_x" and "proj_y"
                     * attributes of a marker inside a track. */
                    double[] projectedValues = mMap.getProjectedValues(trackPoint.getLatitude(),
                            trackPoint.getLongitude());

                    /* By design, default values are null */
                    if (projectedValues != null) {
                        marker.proj_x = projectedValues[0];
                        marker.proj_y = projectedValues[1];
                    }

                    route.route_markers.add(marker);
                }
            }
            return route;
        }
    }
}
