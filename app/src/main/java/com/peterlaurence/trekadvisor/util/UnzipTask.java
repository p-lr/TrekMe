package com.peterlaurence.trekadvisor.util;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Utility class to unzip
 *
 * @author peterLaurence on 12/06/16.
 */
public class UnzipTask extends AsyncTask<Void, Integer, Void> {
    public interface UnzipProgressionListener {
        void onProgress(int p);

        /**
         * Called once the extraction is done.
         * @param outputDirectory the (just created) parent folder
         */
        void onFinished(File outputDirectory);
    }

    private File mZipFile;
    private File mOutputFolder;
    private UnzipProgressionListener mUnzipProgressionListener;

    public UnzipTask(File zipFile, File outputFolder, UnzipProgressionListener listener) {
        mZipFile = zipFile;
        mOutputFolder = outputFolder;
        mUnzipProgressionListener = listener;
    }

    @Override
    protected Void doInBackground(Void... params) {
        byte[] buffer = new byte[1024];

        try {
            /* Create output directory if necessary */
            if (!mOutputFolder.exists()) {
                mOutputFolder.mkdir();
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
                    if (entry.isDirectory()) {
                        newFile.mkdirs();
                        continue;
                    }
                    FileOutputStream fos = new FileOutputStream(newFile);

                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }

                    publishProgress((int) ((entryCount / (float) totalEntries) * 100));

                    fos.close();
                } catch (IOException e) {
                    // something went wrong during extraction
                }
            }

            zis.closeEntry();
            zis.close();
        } catch (IOException ex) {
            // don't care
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        mUnzipProgressionListener.onProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(Void result) {
        mUnzipProgressionListener.onFinished(mOutputFolder);
    }
}
