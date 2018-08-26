package com.peterlaurence.trekadvisor.core.track

import com.peterlaurence.trekadvisor.util.FileUtils
import java.io.File

object TrackTools {
    fun renameTrack(record: File, newName: String): Boolean {
        return try {
            /* Rename the file */
            record.renameTo(File(record.parent, newName + "." + FileUtils.getFileExtension(record)))

            //TODO if the file contains only one track, rename one with the same name

            true
        } catch (e: Exception) {
            false
        }
    }
}