package com.peterlaurence.trekme.core.map.data.models;

import java.io.File;

/**
 * A {@code MapArchive} contains every information about a map archive.
 *
 * @author P.Laurence on 08/06/16.
 */
public class MapArchive {
    private final File mMapArchiveFile;

    /**
     * A {@code MapArchive} is for instance just a json {@link File} and an output directory.
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

    public int getId() {
        return mMapArchiveFile.getPath().hashCode();
    }

    public File getArchiveFile() {
        return mMapArchiveFile;
    }
}
