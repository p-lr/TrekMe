package com.peterlaurence.trekme.core.map.maploader.tasks;

import android.os.Handler;
import android.os.Looper;

import com.peterlaurence.trekme.core.map.MapArchive;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds maps archives, as {@link MapArchive} list, in the provided list of folders to took into. <br>
 * For instance, it only looks for zip files and don't check whether those are real map archives or
 * not. But attempting to extract a wrong file is correctly reported to the user.
 *
 * @author peterLaurence on 30/04/17.
 */
public class MapArchiveSearchTask extends Thread {
    private static final int MAX_RECURSION_DEPTH = 6;
    private static final List<String> mArchiveFormatList;
    private MapLoader.MapArchiveListUpdateListener mMapArchiveUpdateListener;
    private List<MapArchive> mMapArchiveList;
    private List<File> mMapArchiveFilesFoundList;
    private File[] mFoldersToLookInto;

    static {
        mArchiveFormatList = new ArrayList<>();
        mArchiveFormatList.add("zip");
    }

    public MapArchiveSearchTask(MapLoader.MapArchiveListUpdateListener listener,
                                List<MapArchive> mapArchiveList, File... dirsToLookInto) {
        super();
        mMapArchiveList = mapArchiveList;
        mMapArchiveFilesFoundList = new ArrayList<>();
        mMapArchiveUpdateListener = listener;
        mFoldersToLookInto = dirsToLookInto;
    }

    @Override
    public void run() {
        super.run();
        /* Search for archive files on SD card */
        for (File dir : mFoldersToLookInto) {
            findArchives(dir, 1);
        }

        for (File archiveFile : mMapArchiveFilesFoundList) {
            mMapArchiveList.add(new MapArchive(archiveFile));
        }

        /* Run on UI thread */
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            if (mMapArchiveUpdateListener != null) {
                mMapArchiveUpdateListener.onMapArchiveListUpdate();
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
