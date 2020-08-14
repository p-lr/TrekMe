package com.peterlaurence.trekme.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import java.io.FileInputStream
import java.io.OutputStream

/**
 * Creates a thumbnail from the file defined by the [imageUri] and writes the resulting thumbnail to
 * the given [outputStream]. This is a _blocking_ call in the sense of it does IO in the calling
 * thread.
 *
 * @return A compressed [Bitmap] (thumbnail), or null if any error occurred.
 */
fun makeThumbnail(imageUri: Uri, resolver: ContentResolver, thumbnailSize: Int, outputStream: OutputStream): Bitmap? {
    try {
        val parcelFileDescriptor = resolver.openFileDescriptor(imageUri, "r") ?: return null
        val fileDescriptor = parcelFileDescriptor.fileDescriptor
        val fileInputStream = FileInputStream(fileDescriptor)
        val thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(fileInputStream),
                thumbnailSize, thumbnailSize)
        val resultOk = thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return if (resultOk) thumbnail else null
    } catch (e: Exception) {
        return null
    }
}