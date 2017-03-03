package com.peterlaurence.trekadvisor.core.track;

import com.peterlaurence.trekadvisor.util.FileUtils;

import java.io.File;

/**
 * Manage operations related to track import.
 *
 * @author peterLaurence on 03/03/17.
 */
public class TrackImporter {
    private static final String[] supportedTrackFilesExtensions = new String[] {
            "gpx", "json", "xml"
    };

    /* Don't allow instantiation */
    private TrackImporter() {
    }

    public static boolean isFileSupported(File file) {
        String extension = FileUtils.getFileExtension(file);

        if ("".equals(extension)) return false;

        for (String ext : supportedTrackFilesExtensions) {
            if (ext.equals(extension)) return true;
        }
        return false;
    }


}
