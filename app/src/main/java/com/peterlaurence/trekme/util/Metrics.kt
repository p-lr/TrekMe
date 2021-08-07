package com.peterlaurence.trekme.util

import android.content.res.Resources

/**
 * Convert px to dp
 */
fun pxToDp(px: Int): Int = (px / Resources.getSystem().displayMetrics.density).toInt()

/**
 * Convert dp to px
 */
fun dpToPx(dp: Float): Float = dp * Resources.getSystem().displayMetrics.density