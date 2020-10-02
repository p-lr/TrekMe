package com.peterlaurence.trekme.core.map.maploader.tasks;

import android.os.Handler;
import android.os.Looper;

import com.peterlaurence.trekme.core.map.MapArchive;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Finds maps archives, as {@link MapArchive} list, in the provided list of folders to took into. <br>
 * For instance, it only looks for zip files and don't check whether those are real map archives or
 * not. But attempting to extract a wrong file is correctly reported to the user.
 *
 * @author P.Laurence on 30/04/17.
 */
public class MapArchiveSearchTask extends Thread {
    private static final int MAX_RECURSION_DEPTH = 6;
    private static final List<String> mArchiveFormatList;
    private MapLoader.MapArchiveListUpdateListener mMapArchiveUpdateListener;
    private List<File> mMapArchiveFilesFoundList;
    private List<File> mFoldersToLookInto;
    private Boolean isCancelled = false;

    static {
        mArchiveFormatList = new ArrayList<>();
        mArchiveFormatList.add("zip");
    }

    public MapArchiveSearchTask(List<File> dirsToLookInto,
                                MapLoader.MapArchiveListUpdateListener callback) {
        super();
        mMapArchiveFilesFoundList = new ArrayList<>();
        mMapArchiveUpdateListener = callback;
        mFoldersToLookInto = dirsToLookInto;
    }

    public void cancel() {
        isCancelled = true;
    }

    @Override
    public void run() {
        super.run();
        /* Search for archive files on SD card */
        for (File dir : mFoldersToLookInto) {
            findArchives(dir, 1);
        }

        List<MapArchive> mapArchiveList = new ArrayList<>();
        for (File archiveFile : mMapArchiveFilesFoundList) {
            mapArchiveList.add(new MapArchive(archiveFile));
        }

        /* Run on UI thread */
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            if (mMapArchiveUpdateListener != null) {
                mMapArchiveUpdateListener.onMapArchiveListUpdate(Collections.unmodifiableList(mapArchiveList));
            }
        });
    }

    private void findArchives(File root, int depth) {
        if (depth > MAX_RECURSION_DEPTH) return;

        File[] list = root.listFiles();
        if (list == null) {
            return;
        }

        for (File f : list) {
            if (isCancelled) break;
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
}
