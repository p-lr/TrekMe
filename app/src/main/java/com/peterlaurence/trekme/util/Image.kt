package com.peterlaurence.trekme.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
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
    return try {
        val parcelFileDescriptor = resolver.openFileDescriptor(imageUri, "r") ?: return null
        parcelFileDescriptor.use {
            val fileDescriptor = it.fileDescriptor
            val fileInputStream = FileInputStream(fileDescriptor)
            val thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(fileInputStream),
                thumbnailSize, thumbnailSize)
            val resultOk = thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            if (resultOk) thumbnail else null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Converts any drawable into a [Bitmap].
 * The intrinsic dimensions of the drawable must be greater than 0, or this function returns null.
 */
fun getBitmapFromDrawable(drawable: Drawable): Bitmap? {
    val width = drawable.intrinsicWidth
    val height = drawable.intrinsicHeight
    return if (width > 0 && height > 0) {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp
    } else null
}