package com.peterlaurence.trekadvisor.core.map.maploader.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MarkerGson;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;
import com.peterlaurence.trekadvisor.util.FileUtils;

import java.io.File;
import java.util.List;

/**
 * This task is run when this is the first time a map is loaded, so the list of
 * {@link com.peterlaurence.trekadvisor.core.map.gson.MarkerGson.Marker} is required. <br>
 * A file named 'markers.json' is expected at the same level of the 'map.json' configuration file.
 * If there is no markers file, this means that the map has no markers.
 *
 * @author peterLaurence on 30/04/17.
 */
public class MapMarkerImportTask extends AsyncTask<Void, Void, Void> {
    private List<MapLoader.MapMarkerUpdateListener> mListenerList;
    private Map mMap;
    private Gson mGson;
    private static final String TAG = "MapMarkerImportTask";

    public MapMarkerImportTask(List<MapLoader.MapMarkerUpdateListener> listenerList, Map map,
                               Gson gson) {
        super();
        mListenerList = listenerList;
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
        if (mListenerList != null) {
            for (MapLoader.MapMarkerUpdateListener listener : mListenerList) {
                listener.onMapMarkerUpdate();
            }
        }
    }
}
