package com.peterlaurence.trekme.util

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintWriter

fun writeToFile(st: String, out: File, errCb: () -> Unit) {
    try {
        PrintWriter(out).use {
            it.print(st)
        }
    } catch (e: IOException) {
        errCb()
    }
}

fun writeToFile(st: String, out: File): Result<Unit> {
    return runCatching {
        PrintWriter(out).use {
            it.print(st)
        }
    }
}

/**
 * Reads an [Uri] and invokes the provided [reader].
 *
 * @throws FileNotFoundException,
 */
suspend fun <T> readUri(uri: Uri, contentResolver: ContentResolver, reader: suspend (FileInputStream) -> T): T? {
    val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
    return parcelFileDescriptor?.use {
        val fileDescriptor = parcelFileDescriptor.fileDescriptor
        FileInputStream(fileDescriptor).use { fileInputStream ->
            reader(fileInputStream)
        }
    }
}
