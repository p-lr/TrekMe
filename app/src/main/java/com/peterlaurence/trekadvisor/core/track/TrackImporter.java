package com.peterlaurence.trekadvisor.core.track;

import android.os.AsyncTask;

import com.peterlaurence.trekadvisor.core.map.gson.MapGson;
import com.peterlaurence.trekadvisor.util.FileUtils;

import java.io.File;

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
        void onTrackFileParsed(MapGson.Track track);
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
     * Parse a {@link File} that represents a tracks, and is in one of the supported formats. <br>
     * The parsing is done in an asynctask.
     *
     * @param trackFiles a {@link File}
     * @param listener a {@link TrackFileParsedListener}
     */
    public static void parseTrackFile(File[] trackFiles, TrackFileParsedListener listener) {
        GpxTrackFileTask gpxTrackFileTask = new GpxTrackFileTask(listener);
        gpxTrackFileTask.execute(trackFiles);
    }

    private static class GpxTrackFileTask extends AsyncTask<File, Void, MapGson.Track> {
        private TrackFileParsedListener mListener;

        GpxTrackFileTask(TrackFileParsedListener listener) {
            mListener = listener;
        }

        @Override
        protected MapGson.Track doInBackground(File... trackFiles) {
            //TODO : use GPX parser here
            return null;
        }

        @Override
        protected void onPostExecute(MapGson.Track track) {
            mListener.onTrackFileParsed(track);
        }
    }
}
