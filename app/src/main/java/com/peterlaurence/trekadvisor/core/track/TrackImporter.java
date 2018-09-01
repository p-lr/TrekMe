package com.peterlaurence.trekadvisor.core.track;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;

import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MarkerGson;
import com.peterlaurence.trekadvisor.core.map.gson.RouteGson;
import com.peterlaurence.trekadvisor.core.projection.Projection;
import com.peterlaurence.trekadvisor.util.gpx.GPXParser;
import com.peterlaurence.trekadvisor.util.gpx.model.Gpx;
import com.peterlaurence.trekadvisor.util.gpx.model.Track;
import com.peterlaurence.trekadvisor.util.gpx.model.TrackPoint;
import com.peterlaurence.trekadvisor.util.gpx.model.TrackSegment;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import static com.peterlaurence.trekadvisor.core.TrekAdvisorContext.DEFAULT_RECORDINGS_DIR;

/**
 * Utility toolbox to :
 * <ul>
 * <li>Import a gpx track file into a {@link Map}.</li>
 * <li>Get the list of gpx files created by location recording.</li>
 * </ul>
 *
 * @author peterLaurence on 03/03/17.
 */
public abstract class TrackImporter {
    private static final String[] supportedTrackFilesExtensions = new String[]{
            "gpx", "xml"
    };

    private static final FilenameFilter SUPPORTED_FILE_FILTER = (dir, filename) -> {
        /* We only look at files */
        if (new File(dir, filename).isDirectory()) {
            return false;
        }

        boolean accept = true;
        for (final String ext : supportedTrackFilesExtensions) {
            if (!filename.endsWith("." + ext)) {
                accept = false;
            }
            if (accept) return true;
        }
        return false;
    };

    public static boolean isFileSupported(Uri uri) {
        String path = uri.getPath();
        String extension = path.substring(path.lastIndexOf(".") + 1);

        if ("".equals(extension)) return false;

        for (String ext : supportedTrackFilesExtensions) {
            if (ext.equals(extension)) return true;
        }
        return false;
    }

    /**
     * Parse a {@link File} that contains routes, and is in one of the supported formats. <br>
     * The parsing is done in an asynctask.
     *
     * @param uri      the track as an {@link Uri}
     * @param listener a {@link TrackFileParsedListener}
     * @param map      the {@link Map} to which the routes will be added.
     */
    public static void importTrackUri(Uri uri, TrackFileParsedListener listener, Map map,
                                      ContentResolver contentResolver) {

        try {
            ParcelFileDescriptor parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r");
            if (parcelFileDescriptor == null) {
                listener.onError("Could not read content of file");
                return;
            }
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            FileInputStream fileInputStream = new FileInputStream(fileDescriptor);

            GpxTrackFileTask gpxTrackFileTask = new GpxTrackFileTask(listener, map, () ->
            {
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            gpxTrackFileTask.execute(fileInputStream);
        } catch (IOException e) {
            listener.onError("Error when opening the file");
        }
    }

    public static void importTrackFile(File file, TrackFileParsedListener listener, Map map) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);

            GpxTrackFileTask gpxTrackFileTask = new GpxTrackFileTask(listener, map, null);
            gpxTrackFileTask.execute(fileInputStream);
        } catch (FileNotFoundException e) {
            listener.onError("The file doesn't exists");
        }
    }

    /**
     * Get the list of {@link File} which extension is in the list of supported extension for track
     * file. Files are searched into the
     * {@link com.peterlaurence.trekadvisor.core.TrekAdvisorContext#DEFAULT_RECORDINGS_DIR}.
     */
    @Nullable
    public static File[] getRecordings() {
        return DEFAULT_RECORDINGS_DIR.listFiles(SUPPORTED_FILE_FILTER);
    }

    public interface TrackFileParsedListener {
        void onTrackFileParsed(Map map, List<RouteGson.Route> routeList);

        void onError(String message);
    }

    private static class GpxTrackFileTask extends AsyncTask<InputStream, Void, Void> {
        private TrackFileParsedListener mListener;
        private Map mMap;
        private LinkedList<RouteGson.Route> mNewRouteList;
        private Runnable mPostExecuteTask;

        GpxTrackFileTask(TrackFileParsedListener listener, Map map, Runnable postExecuteTask) {
            mListener = listener;
            mMap = map;
            mNewRouteList = new LinkedList<>();
            mPostExecuteTask = postExecuteTask;
        }

        /**
         * Each gpx file may contain several tracks. And each {@link Track} may contain several
         * {@link TrackSegment}. <br>
         * A {@link Track} is the equivalent of a {@link RouteGson.Route}, so all {@link TrackSegment}
         * are added to a single {@link RouteGson.Route}.
         */
        @Override
        protected Void doInBackground(InputStream... inputStreamList) {
            for (InputStream stream : inputStreamList) {

                try {
                    Gpx gpx = GPXParser.INSTANCE.parse(stream);

                    for (Track track : gpx.getTracks()) {
                        RouteGson.Route route = gpxTracktoRoute(track);
                        mNewRouteList.add(route);
                    }
                    stream.close();
                } catch (XmlPullParserException | IOException | ParseException e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    mListener.onError(sw.toString());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mListener.onTrackFileParsed(mMap, mNewRouteList);
            if (mPostExecuteTask != null) {
                mPostExecuteTask.run();
            }
        }

        /**
         * Converts a {@link Track} into a {@link RouteGson.Route}. <br>
         * A single {@link Track} may contain several {@link TrackSegment}.
         */
        private RouteGson.Route gpxTracktoRoute(Track track) {
            /* Create a new route */
            RouteGson.Route route = new RouteGson.Route();

            /* The route name is the track name */
            route.name = track.getName();

            /* All track segments are concatenated */
            List<TrackSegment> trackSegmentList = track.getTrackSegments();
            for (TrackSegment trackSegment : trackSegmentList) {
                List<TrackPoint> trackPointList = trackSegment.getTrackPoints();
                for (TrackPoint trackPoint : trackPointList) {
                    MarkerGson.Marker marker = new MarkerGson.Marker();

                    /* If the map uses a projection, store projected values */
                    double[] projectedValues;
                    Projection projection = mMap.getProjection();
                    if (projection != null) {
                        projectedValues = projection.doProjection(trackPoint.getLatitude(), trackPoint.getLongitude());
                        if (projectedValues != null) {
                            marker.proj_x = projectedValues[0];
                            marker.proj_y = projectedValues[1];
                        }
                    }

                    /* In any case, we store the wgs84 coordinates */
                    marker.lat = trackPoint.getLatitude();
                    marker.lon = trackPoint.getLongitude();

                    route.route_markers.add(marker);
                }
            }
            return route;
        }
    }
}
