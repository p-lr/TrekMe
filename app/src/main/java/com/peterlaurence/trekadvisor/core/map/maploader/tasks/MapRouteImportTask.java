package com.peterlaurence.trekadvisor.core.map.maploader.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.RouteGson;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;
import com.peterlaurence.trekadvisor.util.FileUtils;

import java.io.File;
import java.util.List;

/**
 * This task is run when this is the first time a map is loaded, hence the list of
 * {@link com.peterlaurence.trekadvisor.core.map.gson.RouteGson.Route} is required. <br>
 * A file named 'routes.json' (also referred as track file) is expected at the same level of the
 * 'map.json' configuration file. If there is no track file, this means that the map has no routes.
 *
 * @author peterLaurence on 13/05/17.
 */
public class MapRouteImportTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "MapRouteImportTask";
    private List<MapLoader.MapRouteUpdateListener> mListenerList;
    private Map mMap;
    private Gson mGson;

    public MapRouteImportTask(List<MapLoader.MapRouteUpdateListener> listenerList, Map map, Gson gson) {
        mListenerList = listenerList;
        mMap = map;
        mGson = gson;
    }

    @Override
    protected Void doInBackground(Void... params) {
        File routeFile = new File(mMap.getDirectory(), MapLoader.MAP_ROUTE_FILE_NAME);
        if (!routeFile.exists()) return null;

        String jsonString;
        try {
            jsonString = FileUtils.getStringFromFile(routeFile);
            RouteGson routeGson = mGson.fromJson(jsonString, RouteGson.class);
            mMap.setRouteGson(routeGson);
        } catch (Exception e) {
            /* Error while decoding the json file */
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (mListenerList != null) {
            for (MapLoader.MapRouteUpdateListener listener : mListenerList) {
                listener.onMapRouteUpdate();
            }
        }
    }
}
