package com.peterlaurence.trekme.util

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


/**
 * Utility function to zip a map, but can be used with any folder. This is a blocking call - this is
 * the responsibility of the calling code to execute this in the right context.
 *
 * @param folderToZip The directory to archive.
 * @param outputStream The stream in which the archive will be written.
 * @param listener    The [ZipProgressionListener] will be called back.
 *
 * @author peterLaurence on 31/07/17.
 */
fun zipTask(folderToZip: File, outputStream: OutputStream, listener: ZipProgressionListener) {
    /* Get the list of files in the archive */
    val filePathList = ArrayList<String>()
    getFileList(folderToZip, filePathList)
    listener.fileListAcquired()

    try {
        val zos = ZipOutputStream(outputStream)

        var entryCount = 0
        val totalEntries = filePathList.size
        val buffer = ByteArray(1024)
        for (filePath in filePathList) {
            entryCount++
            /* Create a zip entry */
            val name = filePath.substring(folderToZip.absolutePath.length + 1,
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

            listener.onProgress((entryCount / totalEntries.toFloat() * 100).toInt())

            /* Close the zip entry and the file input stream */
            zos.closeEntry()
            fis.close()
        }
        zos.close()
    } catch (e: IOException) {
        Log.e(TAG, stackTraceToString(e))
        return listener.onZipError()
    } finally {
        outputStream.close()
    }
    listener.onZipFinished()
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
