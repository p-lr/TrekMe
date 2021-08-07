package com.peterlaurence.trekme.util

import android.content.res.Resources

/**
 * Convert px to dp
 */
fun pxToDp(px: Int): Int = (px / Resources.getSystem().displayMetrics.density).toInt()

/**
 * Convert dp to px
 * TODO: refactor to a function named dpToPx
 */
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()