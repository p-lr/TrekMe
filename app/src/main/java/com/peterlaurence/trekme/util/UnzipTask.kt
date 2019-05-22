package com.peterlaurence.trekme.util

import android.util.Log

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

private const val TAG = "UnzipTask"

fun unzipTask(zipFile: File, outputFolder: File, unzipProgressionListener: UnzipProgressionListener) {
    val buffer = ByteArray(1024)
    var result = true

    try {
        /* Create output directory if necessary */
        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }

        val zip = ZipFile(zipFile)
        val totalEntries = zip.size().toLong()
        var entryCount = 0

        val zis = ZipInputStream(FileInputStream(zipFile))

        while (true) {
            val entry = zis.nextEntry ?: break
            entryCount++
            val fileName = entry.name
            val newFile = File(outputFolder, fileName)
            if (!newFile.canonicalPath.startsWith(outputFolder.canonicalPath)) {
                /* Protect from zip traversal vulnerability */
                throw SecurityException()
            }

            try {
                if (!newFile.exists()) {
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                        continue
                    } else {
                        newFile.parentFile.mkdirs()
                        newFile.createNewFile()
                    }
                }

                val fos = FileOutputStream(newFile)

                while (true) {
                    val len = zis.read(buffer)
                    if (len <= 0)
                        break
                    fos.write(buffer, 0, len)
                }

                unzipProgressionListener.onProgress((entryCount / totalEntries.toFloat() * 100).toInt())

                fos.close()
            } catch (e: IOException) {
                /* Something went wrong during extraction */
                Log.e(TAG, stackTraceToString(e))
                result = false
            }
        }

        zis.closeEntry()
        zis.close()
    } catch (ex: IOException) {
        Log.e(TAG, stackTraceToString(ex))
        result = false
    } catch (e: SecurityException) {
        Log.e(TAG, "Zip traversal vulnerability tried to be exploited")
        result = false
    }

    if (result) {
        unzipProgressionListener.onUnzipFinished(outputFolder)
    } else {
        unzipProgressionListener.onUnzipError()
    }
}

interface UnzipProgressionListener {
    fun onProgress(p: Int)

    /**
     * Called once the extraction is done.
     *
     * @param outputDirectory the (just created) parent folder
     */
    fun onUnzipFinished(outputDirectory: File)

    /**
     * Called whenever an error happens during extraction.
     */
    fun onUnzipError()
}
