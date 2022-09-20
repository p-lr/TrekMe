package com.peterlaurence.trekme.core.map.data.utils

import com.peterlaurence.trekme.util.UnzipProgressionListener
import com.peterlaurence.trekme.util.unzipTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * Unzips the given [inputStream] (by creating a [ZipInputStream] and reading from it).
 * The output folder must be provided, along with:
 * * A name, which is used as prefix for the direct parent folder of the map. If the file already
 * exists, a dash with a number is appended.
 * * The size in bytes of the document being read.
 *
 * @author P.Laurence on 28/02/20
 */
suspend fun unarchive(inputStream: InputStream, outputDirectory: File, name: String, size: Long,
                      listener: UnzipProgressionListener) = withContext(Dispatchers.IO) {
    /* Generate an output directory  */
    val parentFolderName = name.removeSuffix(".zip")
    val intermediateDirectory = uniqueFile(File(outputDirectory, parentFolderName))

    /* Launch the unzip task */
    unzipTask(inputStream, intermediateDirectory, size, true, listener)
}

/**
 * Appends a dash and a number so that the returned [File] is guaranteed to not exist (unique file).
 * Uses a recursive algorithm.
 */
private tailrec fun uniqueFile(file: File): File {
    return if (!file.exists()) {
        file
    } else {
        val indexStr = file.name.substringAfterLast('-', "")
        val index = if (indexStr.isNotEmpty()) {
            try {
                indexStr.toInt()
            } catch (e: Exception) {
                1
            }
        } else 1
        val baseName = file.name.substringBeforeLast('-')

        val newFile = File(file.parent, "$baseName-${index + 1}")
        uniqueFile(newFile)
    }
}
