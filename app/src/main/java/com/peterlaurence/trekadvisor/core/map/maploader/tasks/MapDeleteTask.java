package com.peterlaurence.trekadvisor.core.map.maploader.tasks;

import android.os.AsyncTask;

import com.peterlaurence.trekadvisor.util.FileUtils;

import java.io.File;

/**
 * @author peterLaurence on 16/07/17.
 */
public class MapDeleteTask extends AsyncTask<File, Void, Void> {
    private File mMapDirectory;

    public MapDeleteTask(File mapDirectory) {
        mMapDirectory = mapDirectory;
    }

    @Override
    protected Void doInBackground(File... params) {
        FileUtils.deleteRecursive(mMapDirectory);
        return null;
    }
}
