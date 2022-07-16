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

/**
 * Reads an [Uri] and invokes the provided [reader].
 *
 * @throws FileNotFoundException,
 */
inline fun <T> readUri(uri: Uri, contentResolver: ContentResolver, reader: (FileInputStream) -> T): T? {
    val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
    return parcelFileDescriptor?.use {
        val fileDescriptor = parcelFileDescriptor.fileDescriptor
        FileInputStream(fileDescriptor).use { fileInputStream ->
            reader(fileInputStream)
        }
    }
}
