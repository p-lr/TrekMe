package com.peterlaurence.trekadvisor.util;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Utility class used to unzip any zip file.
 *
 * @author peterLaurence on 12/06/16.
 */
public class UnzipTask extends Thread {
    private static final String TAG = "UnzipTask";
    private File mZipFile;
    private File mOutputFolder;
    private UnzipProgressionListener mUnzipProgressionListener;

    public UnzipTask(File zipFile, File outputFolder, UnzipProgressionListener listener) {
        mZipFile = zipFile;
        mOutputFolder = outputFolder;
        mUnzipProgressionListener = listener;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        boolean result = true;

        try {
            /* Create output directory if necessary */
            if (!mOutputFolder.exists()) {
                mOutputFolder.mkdirs();
            }

            ZipFile zip = new ZipFile(mZipFile);
            long totalEntries = zip.size();
            int entryCount = 0;

            ZipInputStream zis =
                    new ZipInputStream(new FileInputStream(mZipFile));

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                String fileName = entry.getName();
                File newFile = new File(mOutputFolder, fileName);

                try {
                    if (!newFile.exists()) {
                        if (entry.isDirectory()) {
                            newFile.mkdirs();
                            continue;
                        } else {
                            newFile.getParentFile().mkdirs();
                            newFile.createNewFile();
                        }
                    }

                    FileOutputStream fos = new FileOutputStream(newFile);

                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }

                    mUnzipProgressionListener.onProgress((int) ((entryCount / (float) totalEntries) * 100));

                    fos.close();
                } catch (IOException e) {
                    /* Something went wrong during extraction */
                    Log.e(TAG, Tools.stackTraceToString(e));
                    result = false;
                }
            }

            zis.closeEntry();
            zis.close();
        } catch (IOException ex) {
            Log.e(TAG, Tools.stackTraceToString(ex));
            result = false;
        }

        if (result) {
            mUnzipProgressionListener.onUnzipFinished(mOutputFolder);
        } else {
            mUnzipProgressionListener.onUnzipError();
        }
    }


    public interface UnzipProgressionListener {
        void onProgress(int p);

        /**
         * Called once the extraction is done.
         *
         * @param outputDirectory the (just created) parent folder
         */
        void onUnzipFinished(File outputDirectory);

        /**
         * Called whenever an error happens during extraction.
         */
        void onUnzipError();
    }
}
