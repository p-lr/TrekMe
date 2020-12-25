package com.peterlaurence.trekme.util

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

private const val TAG = "UnzipTask"

/**
 * Unzips from an [InputStream] into an [outputFolder].
 * To compute progress and callback the provided [unzipProgressionListener] with correct values, this
 * function also expects the size of the input stream as [size] parameter. As this size might be the
 * compressed or uncompressed size of the stream, the [isInitialized] flag is expected.
 */
fun unzipTask(inputStream: InputStream, outputFolder: File, size: Long, isSizeCompressed: Boolean,
              unzipProgressionListener: UnzipProgressionListener) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var result = true

    try {
        /* Create output directory if necessary */
        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }

        var bytesRead = 0L
        val zis = ZipInputStream(inputStream)
        var progress = -1
        while (true) {
            val entry = zis.nextEntry ?: break
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
                        newFile.parentFile?.mkdirs()
                        newFile.createNewFile()
                    }
                }

                val fos = FileOutputStream(newFile)

                /* We don't rely on entry.size to get the actual number of bytes read */
                var bytesEntry = 0L
                while (true) {
                    val len = zis.read(buffer)
                    if (len < 0)
                        break
                    fos.write(buffer, 0, len)
                    bytesEntry += len
                }

                bytesRead += if (isSizeCompressed) entry.compressedSize else bytesEntry
                val newProgress = (bytesRead / size.toDouble() * 100).toInt()
                if (newProgress != progress) {
                    unzipProgressionListener.onProgress(newProgress)
                    progress = newProgress
                }

                fos.close()
            } catch (e: IOException) {
                /* Something went wrong during extraction */
                Log.e(TAG, stackTraceToString(e))
                result = false
            }
        }

        zis.closeEntry()
        runCatching {
            zis.close()
        }
    } catch (ex: IOException) {
        Log.e(TAG, stackTraceToString(ex))
        result = false
    } catch (e: SecurityException) {
        Log.e(TAG, "Zip traversal vulnerability tried to be exploited")
        result = false
    } catch (e: IllegalArgumentException) { // Although not documented, ZipInputStream.getNextEntry() can throw IllegalArgumentException
        Log.e(TAG, stackTraceToString(e))
        result = false
    }

    if (result) {
        unzipProgressionListener.onUnzipFinished(outputFolder)
    } else {
        unzipProgressionListener.onUnzipError()
    }
}

interface UnzipProgressionListener {
    /**
     * Provides the progress in percent.
     */
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
