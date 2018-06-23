package com.peterlaurence.trekadvisor.core;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

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
    public static final String APP_FOLDER_NAME = "trekadvisor";
    public static final File DEFAULT_APP_DIR = new File(Environment.getExternalStorageDirectory(),
            APP_FOLDER_NAME);
    /* For instance maps are searched anywhere under the app folder */
    public static final File DEFAULT_MAPS_DIR = DEFAULT_APP_DIR;

    public static final File DEFAULT_MAPS_DOWNLOAD_DIR = new File(DEFAULT_MAPS_DIR, "downloaded");
    public static final File DEFAULT_RECORDINGS_DIR = new File(DEFAULT_APP_DIR, "recordings");
    public static final File CREDENTIALS_DIR = new File(DEFAULT_APP_DIR, "credentials");
    private static final String TAG = "TrekAdvisorContext";

    /**
     * Create necessary folders and files.
     */
    public static void init() {
        try {
            createAppDirs();
            createNomediaFile();
        } catch (SecurityException | IOException e) {
            Log.e(TAG, "We don't have right access to create application folder");
        }
    }

    private static void createAppDirs() throws SecurityException {
        /* Root */
        createDir(DEFAULT_APP_DIR, "application");

        /* Credentials */
        createDir(CREDENTIALS_DIR, "credentials");
    }

    private static void createDir(File dir, String label) {
        if (!dir.exists()) {
            boolean created = dir.mkdir();
            if (!created) {
                Log.e(TAG, "Could not create " + label + " folder");
            }
        }
    }

    /**
     * We have to create an empty ".nomedia" file at the root of the application folder, so other
     * apps don't index this content for media files.
     */
    private static void createNomediaFile() throws SecurityException, IOException {
        if (DEFAULT_APP_DIR.exists()) {
            File noMedia = new File(DEFAULT_APP_DIR, ".nomedia");
            boolean created = noMedia.createNewFile();
            if (!created) {
                Log.e(TAG, "Could not create .nomedia file");
            }
        }
    }
}
