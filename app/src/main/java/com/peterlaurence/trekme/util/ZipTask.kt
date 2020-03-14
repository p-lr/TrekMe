package com.peterlaurence.trekme.util

import android.os.AsyncTask
import android.util.Log
import com.peterlaurence.trekme.util.ZipTask.ZipProgressionListener
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


/**
 * Utility class to zip a map, but can be used with any folder.
 *
 * @param mFolderToZip The directory to archive.
 * @param outputStream The stream in which the archive will be written.
 * @param mZipProgressionListener    The [ZipProgressionListener] will be called back.
 *
 * @author peterLaurence on 31/07/17.
 */
class ZipTask(private val mFolderToZip: File,
              private val outputStream: OutputStream,
              private val mZipProgressionListener: ZipProgressionListener) : AsyncTask<Void, Int, Boolean>() {

    override fun doInBackground(vararg params: Void): Boolean? {

        /* Get the list of files in the archive */
        val filePathList = ArrayList<String>()
        getFileList(mFolderToZip, filePathList)
        mZipProgressionListener.fileListAcquired()

        try {
            val zos = ZipOutputStream(outputStream)

            var entryCount = 0
            val totalEntries = filePathList.size
            val buffer = ByteArray(1024)
            for (filePath in filePathList) {
                entryCount++
                /* Create a zip entry */
                val name = filePath.substring(mFolderToZip.absolutePath.length + 1,
                        filePath.length)
                val zipEntry = ZipEntry(name)
                zos.putNextEntry(zipEntry)

                /* Read file content and write to zip output stream */
                val fis = FileInputStream(filePath)

                while (true) {
                    val len = fis.read(buffer)
                    if (len <= 0)
                        break
                    zos.write(buffer, 0, len)
                }

                publishProgress((entryCount / totalEntries.toFloat() * 100).toInt())

                /* Close the zip entry and the file input stream */
                zos.closeEntry()
                fis.close()
            }
            zos.close()
        } catch (e: IOException) {
            Log.e(TAG, stackTraceToString(e))
            return false
        } finally {
            outputStream.close()
        }

        return true
    }

    override fun onProgressUpdate(vararg progress: Int?) {
        mZipProgressionListener.onProgress(progress[0]!!)
    }

    override fun onPostExecute(result: Boolean?) {
        if (result!!) {
            mZipProgressionListener.onZipFinished()
        } else {
            mZipProgressionListener.onZipError()
        }
    }

    interface ZipProgressionListener {
        /**
         * Before compression, the list of files in the parent folder is acquired. This step can
         * take some time. <br></br>
         * This is called when this step is finished.
         */
        fun fileListAcquired()

        fun onProgress(p: Int)

        /**
         * Called once the compression is done.
         */
        fun onZipFinished()

        /**
         * Called whenever an error happens during compression.
         */
        fun onZipError()
    }
}

private fun getFileList(directory: File, filePathList: MutableList<String>) {
    val files = directory.listFiles()
    if (files != null && files.isNotEmpty()) {
        for (file in files) {
            if (file.isFile) {
                filePathList.add(file.absolutePath)
            } else {
                getFileList(file, filePathList)
            }
        }
    }
}

private const val TAG = "ZipTask"
