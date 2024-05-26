package com.peterlaurence.trekme.core.map.domain.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Create an empty ".nomedia" file at the root of the map directory. This way, other apps don't
 * index this content for media files.
 */
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun createNomediaFile(directory: File) = withContext(Dispatchers.IO) {
    runCatching {
        val noMedia = File(directory, ".nomedia")
        noMedia.createNewFile()
    }.getOrElse { false }
}