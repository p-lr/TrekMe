package com.peterlaurence.trekadvisor.core.map;

import com.peterlaurence.trekadvisor.util.UnzipTask;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A {@code MapArchive} contains every information about a map archive.
 *
 * @author peterLaurence on 08/06/16.
 */
public class MapArchive {
    private final File mMapArchiveFile;

    /**
     * A {@code MapArchive} is for instance just a json {@link File} and an output directory.
     * For instance, just unzips in a subfolder of the same parent folder of the archive
     * {@link File} passed as parameter. The subfolder is named from a formatting of the current
     * date (see {@link #unZip}).
     *
     * @param archiveFile The json {@link File}
     */
    public MapArchive(File archiveFile) {
        mMapArchiveFile = archiveFile;
    }

    /**
     * Returns the name of the archive file, without the extension.
     */
    public String getName() {
        String name = mMapArchiveFile.getName();
        int pos = name.lastIndexOf(".");
        if (pos > 0) {
            name = name.substring(0, pos);
        }
        return name;
    }

    /**
     * Unzip this {@code MapArchive}.
     *
     * @param listener The {@link UnzipTask.UnzipProgressionListener} to get progression updates
     */
    public void unZip(UnzipTask.UnzipProgressionListener listener) {
        /* Generate an output directory with the date */
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd\\MM\\yyyy-HH:mm:ss", Locale.ENGLISH);
        String parentFolderName = getName() + "-" + dateFormat.format(date);
        File outputDirectory = new File(mMapArchiveFile.getParentFile(), parentFolderName);

        UnzipTask unzipTask = new UnzipTask(mMapArchiveFile, outputDirectory, listener);
        unzipTask.execute();
    }
}
