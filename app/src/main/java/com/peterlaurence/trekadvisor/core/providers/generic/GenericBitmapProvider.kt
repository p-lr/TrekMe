package com.peterlaurence.trekadvisor.core.providers.generic

import android.graphics.Bitmap
import android.graphics.BitmapFactory

interface GenericBitmapProvider {
    fun getBitmap(level: Int, row: Int, col: Int): Bitmap?
    fun setBitmapOptions(options: BitmapFactory.Options)
}