package com.peterlaurence.trekadvisor.core.map.maploader.tasks;

import android.os.AsyncTask;

import java.io.File;

/**
 * This task is run when this is the first time a map is loaded, so the list of
 * {@link com.peterlaurence.trekadvisor.core.map.gson.MarkerGson.Marker} is required. <br>
 * A file named 'markers.json' is expected at the same level of the 'map.json' configuration file.
 * If there is no markers file, this means that the map has no markers.
 *
 * @author peterLaurence on 30/04/17.
 */
public class MapMarkerImportTask extends AsyncTask<File, Void, Void> {

    @Override
    protected Void doInBackground(File... params) {
        return null;
    }
}
