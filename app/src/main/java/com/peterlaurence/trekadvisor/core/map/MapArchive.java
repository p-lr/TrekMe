package com.peterlaurence.trekadvisor.core.map;

import com.peterlaurence.trekadvisor.util.UnzipTask;

import java.io.File;

/**
 * A {@code MapArchive} contains every informations relative to a map archive.
 *
 * @author peterLaurence on 08/06/16.
 */
public class MapArchive {
    private final File mMapArchiveFile;
    private final File mOutputDirectory;

    /**
     * A {@code MapArchive} is for instance just a json {@link File} and an output directory.
     *
     * @param archiveFile The json {@link File}
     * @param outputDir The output directory where the json file is extracted to.
     */
    public MapArchive(File archiveFile, File outputDir) {
        mMapArchiveFile = archiveFile;
        mOutputDirectory = outputDir;
    }

    public String getName() {
        return mMapArchiveFile.getName();
    }

    /**
     * Unzip this {@code MapArchive}.
     * For instance, just unzips in the same directory.
     *
     * @param listener The {@link UnzipTask.UnzipProgressionListener} to get progression updates
     */
    public void unZip(UnzipTask.UnzipProgressionListener listener) {
        UnzipTask unzipTask = new UnzipTask(mMapArchiveFile, mOutputDirectory, listener);
        unzipTask.execute();
    }
}
