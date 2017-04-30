package com.peterlaurence.trekadvisor.core.map.maploader.tasks;

import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.peterlaurence.trekadvisor.core.map.MapArchive;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peterLaurence on 30/04/17.
 */
public class MapArchiveSearchTask extends AsyncTask<File, Void, Void> {
    private static final int MAX_RECURSION_DEPTH = 6;
    private List<MapLoader.MapArchiveListUpdateListener> mMapArchiveListUpdateListeners;
    private List<MapArchive> mMapArchiveList;
    private List<File> mMapArchiveFilesFoundList;
    private static final List<String> mArchiveFormatList;

    static {
        mArchiveFormatList = new ArrayList<>();
        mArchiveFormatList.add("zip");
    }

    public MapArchiveSearchTask(@Nullable List<MapLoader.MapArchiveListUpdateListener> listeners, List<MapArchive> mapArchiveList) {
        super();
        mMapArchiveList = mapArchiveList;
        mMapArchiveFilesFoundList = new ArrayList<>();
        mMapArchiveListUpdateListeners = listeners;
    }

    @Override
    protected Void doInBackground(File... dirs) {
        /* Search for archive files on SD card */
        for (File dir : dirs) {
            findArchives(dir, 1);
        }

        for (File archiveFile : mMapArchiveFilesFoundList) {
            mMapArchiveList.add(new MapArchive(archiveFile));
        }

        return null;
    }

    private void findArchives(File root, int depth) {
        if (depth > MAX_RECURSION_DEPTH) return;

        File[] list = root.listFiles();
        if (list == null) {
            return;
        }

        for (File f : list) {
            if (f.isDirectory()) {
                File jsonFile = new File(f, MapLoader.MAP_FILE_NAME);

                /* Don't allow archives inside maps */
                if (!jsonFile.exists()) {
                    findArchives(f, depth + 1);
                }
            } else {
                int index = f.getName().lastIndexOf('.');
                if (index > 0) {
                    try {
                        String ext = f.getName().substring(index + 1);
                        if (mArchiveFormatList.contains(ext)) {
                            mMapArchiveFilesFoundList.add(f);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        // don't care
                    }
                }
            }
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        if (mMapArchiveListUpdateListeners != null) {
            for (MapLoader.MapArchiveListUpdateListener listener : mMapArchiveListUpdateListeners) {
                listener.onMapArchiveListUpdate();
            }
        }
    }
}
