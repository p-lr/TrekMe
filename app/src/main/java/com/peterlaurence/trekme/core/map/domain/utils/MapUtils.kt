package com.peterlaurence.trekme.core.map.domain.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Create an empty ".nomedia" file at the root of the map directory. This way, other apps don't
 * index this content for media files.
 */
suspend fun createNomediaFile(directory: File) = withContext(Dispatchers.IO) {
    runCatching {
        val noMedia = File(directory, ".nomedia")
        noMedia.createNewFile()
    }.getOrElse { false }
}

suspend fun createDownloadPendingFile(directory: File) = withContext(Dispatchers.IO) {
    runCatching {
        val file = File(directory, DOWNLOAD_PENDING)
        file.createNewFile()
    }.getOrElse { false }
}

suspend fun deleteDownloadPendingFile(directory: File) = withContext(Dispatchers.IO) {
    runCatching {
        val file = File(directory, DOWNLOAD_PENDING)
        file.delete()
    }.getOrElse { false }
}

suspend fun hasDownloadPendingFile(directory: File) = withContext(Dispatchers.IO) {
    runCatching {
        directory.listFiles()?.any {
            it.name == DOWNLOAD_PENDING
        } ?: false
    }.getOrElse { false }
}

private const val DOWNLOAD_PENDING = ".download-pending"