package com.peterlaurence.trekadvisor.core;

import android.os.Environment;

import java.io.File;

/**
 * General context attributes of the application. <br>
 * Here is defined :
 * <ul>
 * <li>The root folder of the application on the external storage</li>
 * <li>Where maps are searched</li>
 * <li>The default folder in which new maps downloaded from the internet are imported</li>
 * </ul>
 *
 * @author peterLaurence on 07/10/17.
 */
public final class TrekAdvisorContext {
    private static final String APP_FOLDER_NAME = "trekadvisor";
    public static final File DEFAULT_APP_DIR = new File(Environment.getExternalStorageDirectory(),
            APP_FOLDER_NAME);
    /* For instance maps are searched anywhere under the app folder */
    public static final File DEFAULT_MAPS_DIR = DEFAULT_APP_DIR;

    public static final File DEFAULT_MAPS_DOWNLOAD_DIR = new File(DEFAULT_MAPS_DIR, "maps");
}
