package com.peterlaurence.trekme.core.map.maparchiver

import com.peterlaurence.trekme.util.UnzipProgressionListener
import com.peterlaurence.trekme.util.unzipTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unzips the given [inputStream] (by creating a [ZipInputStream] and reading from it).
 * The output folder must be provided, along with:
 * * a name, which is used as prefix for the direct parent folder of the map. The current date is
 * appended.
 * * the size in bytes of the document being read
 *
 * @author peterLaurence on 28/02/20
 */
fun CoroutineScope.unarchive(inputStream: InputStream, outputDirectory: File, name: String, size: Long, listener: UnzipProgressionListener) {
    /* Generate an output directory with the date */
    val date = Date()
    val dateFormat = SimpleDateFormat("dd\\MM\\yyyy-HH:mm:ss", Locale.ENGLISH)
    val parentFolderName = name + "-" + dateFormat.format(date)
    val intermediateDirectory = File(outputDirectory, parentFolderName)

    /* Launch the unzip task */
    launch(Dispatchers.IO) {
        unzipTask(inputStream, intermediateDirectory, size, listener)
    }
}
