package com.peterlaurence.trekme.util

import android.content.res.Resources

/**
 * Convert px to dp
 */
val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

/**
 * Convert dp to px
 */
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()