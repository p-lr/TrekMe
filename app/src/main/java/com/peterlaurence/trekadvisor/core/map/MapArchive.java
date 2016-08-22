package com.peterlaurence.trekadvisor.core.map;

import com.peterlaurence.trekadvisor.util.UnzipTask;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A {@code MapArchive} contains every informations relative to a map archive.
 *
 * @author peterLaurence on 08/06/16.
 */
public class MapArchive {
    private final File mMapArchiveFile;
    private static final String IMPORTED_MAP_FOLDER_NAME = "imported";
    private final File mDefaultImportedMapDir;

    /**
     * A {@code MapArchive} is for instance just a json {@link File} and an output directory.
     *
     * @param archiveFile The json {@link File}
     * @param appDir The app directory on SD card, which will be parent of the output directory.
     */
    public MapArchive(File archiveFile, File appDir) {
        mMapArchiveFile = archiveFile;
        mDefaultImportedMapDir = new File(appDir, IMPORTED_MAP_FOLDER_NAME);
    }

    public String getName() {
        return mMapArchiveFile.getName();
    }

    /**
     * Unzip this {@code MapArchive}.
     * For instance, just unzips in a subfolder of {@code mDefaultImportedMapDir}, named by date.
     *
     * @param listener The {@link UnzipTask.UnzipProgressionListener} to get progression updates
     */
    public void unZip(UnzipTask.UnzipProgressionListener listener) {
        /* Generate an output directory with the date */
        Date date = new Date() ;
        DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG);
        File outputDirectory = new File(mDefaultImportedMapDir, dateFormat.format(date));

        UnzipTask unzipTask = new UnzipTask(mMapArchiveFile, outputDirectory, listener);
        unzipTask.execute();
    }
}
