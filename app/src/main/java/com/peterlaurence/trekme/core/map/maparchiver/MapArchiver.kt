package com.peterlaurence.trekme.core.map.maparchiver

import com.peterlaurence.trekme.core.map.MapArchive
import com.peterlaurence.trekme.util.UnzipProgressionListener
import com.peterlaurence.trekme.util.unzipTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * For instance, just unzips in a subfolder of the same parent folder of the archive
 * [File] passed as parameter. The subfolder is named from a formatting of the current
 * date.
 * The provided [listener] is called from a background thread.
 *
 * @author peterLaurence on 14/10/17 --  converted to Kotlin on 18/05/2019
 */
fun CoroutineScope.unarchive(mapArchive: MapArchive, listener: UnzipProgressionListener) {
    /* Generate an output directory with the date */
    val date = Date()
    val dateFormat = SimpleDateFormat("dd\\MM\\yyyy-HH:mm:ss", Locale.ENGLISH)
    val parentFolderName = mapArchive.name + "-" + dateFormat.format(date)
    val zipFile = mapArchive.archiveFile
    val outputDirectory = File(zipFile.parentFile, parentFolderName)

    /* Launch the unzip task */
    launch(Dispatchers.Default) {
        unzipTask(zipFile, outputDirectory, listener)
    }
}
