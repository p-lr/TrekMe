package com.peterlaurence.trekme.core.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Create an empty ".nomedia" file at the root of the map directory. This way, other apps don't
 * index this content for media files.
 */
suspend fun Map.createNomediaFile() = withContext(Dispatchers.IO) {
    val noMedia = File(directory, ".nomedia")
    noMedia.createNewFile()
}