package com.peterlaurence.trekadvisor.core.map.maparchiver;

import com.peterlaurence.trekadvisor.core.map.MapArchive;
import com.peterlaurence.trekadvisor.util.UnzipTask;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class to handler unzipping of a {@link MapArchive}.
 *
 * @author peterLaurence on 14/10/17.
 */
public class MapArchiver {
    /**
     * For instance, just unzips in a subfolder of the same parent folder of the archive
     * {@link File} passed as parameter. The subfolder is named from a formatting of the current
     * date.
     */
    public static void archiveMap(final MapArchive mapArchive, UnzipTask.UnzipProgressionListener listener) {
        /* Generate an output directory with the date */
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd\\MM\\yyyy-HH:mm:ss", Locale.ENGLISH);
        String parentFolderName = mapArchive.getName() + "-" + dateFormat.format(date);
        File zipFile = mapArchive.getArchiveFile();
        File outputDirectory = new File(zipFile.getParentFile(), parentFolderName);

        /* Launch the unzip thread */
        UnzipTask unzipTask = new UnzipTask(zipFile, outputDirectory, listener);
        unzipTask.start();
    }
}
