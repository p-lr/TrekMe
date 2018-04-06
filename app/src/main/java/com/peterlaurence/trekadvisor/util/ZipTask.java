package com.peterlaurence.trekadvisor.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.peterlaurence.trekadvisor.util.ToolsKt.stackTraceToString;

/**
 * Utility class to zip a map, but can be used with any folder.
 *
 * @author peterLaurence on 31/07/17.
 */

public class ZipTask extends AsyncTask<Void, Integer, Boolean> {
    private static final String TAG = "ZipTask";
    private File mFolderToZip;
    private File mOutputFile;
    private ZipProgressionListener mZipProgressionListener;

    /**
     * @param folderToZip The directory to archive.
     * @param outputFile  The zip {@link File} to write into. This file must exist.
     * @param listener    The {@link ZipProgressionListener} will be called back.
     */
    public ZipTask(File folderToZip, File outputFile, ZipProgressionListener listener) {
        mFolderToZip = folderToZip;
        mOutputFile = outputFile;
        mZipProgressionListener = listener;
    }

    private static void getFileList(File directory, List<String> filePathList) {
        File[] files = directory.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    filePathList.add(file.getAbsolutePath());
                } else {
                    getFileList(file, filePathList);
                }
            }
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        /* Get the list of files in the archive */
        List<String> filePathList = new ArrayList<>();
        getFileList(mFolderToZip, filePathList);
        mZipProgressionListener.fileListAcquired();

        try {

            /* Create parent directory if necessary */
            if (!mOutputFile.exists()) {
                if (!mOutputFile.mkdir()) {
                    return false;
                }
            }
            FileOutputStream fos = new FileOutputStream(mOutputFile);
            ZipOutputStream zos = new ZipOutputStream(fos);


            int entryCount = 0;
            int totalEntries = filePathList.size();
            byte[] buffer = new byte[1024];
            for (String filePath : filePathList) {
                entryCount++;
                /* Create a zip entry */
                String name = filePath.substring(mFolderToZip.getAbsolutePath().length() + 1,
                        filePath.length());
                ZipEntry zipEntry = new ZipEntry(name);
                zos.putNextEntry(zipEntry);

                /* Read file content and write to zip output stream */
                FileInputStream fis = new FileInputStream(filePath);

                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }

                publishProgress((int) ((entryCount / (float) totalEntries) * 100));

                /* Close the zip entry and the file input stream */
                zos.closeEntry();
                fis.close();
            }
            zos.close();
        } catch (IOException e) {
            Log.e(TAG, stackTraceToString(e));
            return false;
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        mZipProgressionListener.onProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            mZipProgressionListener.onZipFinished(mOutputFile);
        } else {
            mZipProgressionListener.onZipError();
        }
    }

    public interface ZipProgressionListener {
        /**
         * Before compression, the list of files in the parent folder is acquired. This step can
         * take some time. <br>
         * This is called when this step is finished.
         */
        void fileListAcquired();

        void onProgress(int p);

        /**
         * Called once the compression is done.
         *
         * @param outputDirectory the (just created) parent folder
         */
        void onZipFinished(File outputDirectory);

        /**
         * Called whenever an error happens during compression.
         */
        void onZipError();
    }
}
