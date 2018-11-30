package com.peterlaurence.trekme.core.map.maploader.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.gson.MarkerGson;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.util.FileUtils;

import java.io.File;

/**
 * This task is run when this is the first time a map is loaded, hence the list of
 * {@link com.peterlaurence.trekme.core.map.gson.MarkerGson.Marker} is required. <br>
 * A file named 'markers.json' is expected at the same level of the 'map.json' configuration file.
 * If there is no markers file, this means that the map has no markers.
 *
 * @author peterLaurence on 30/04/17.
 */
public class MapMarkerImportTask extends AsyncTask<Void, Void, Void> {
    private MapLoader.MapMarkerUpdateListener mListener;
    private Map mMap;
    private Gson mGson;
    private static final String TAG = "MapMarkerImportTask";

    public MapMarkerImportTask(MapLoader.MapMarkerUpdateListener listener, Map map,
                               Gson gson) {
        super();
        mListener = listener;
        mMap = map;
        mGson = gson;
    }

    @Override
    protected Void doInBackground(Void... params) {
        File markerFile = new File(mMap.getDirectory(), MapLoader.MAP_MARKER_FILE_NAME);
        if (!markerFile.exists()) return null;

        String jsonString;
        try {
            jsonString = FileUtils.getStringFromFile(markerFile);
            MarkerGson markerGson = mGson.fromJson(jsonString, MarkerGson.class);
            mMap.setMarkerGson(markerGson);
        } catch (Exception e) {
            /* Error while decoding the json file */
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (mListener != null) {
            mListener.onMapMarkerUpdate();
        }
    }
}
