package com.peterlaurence.trekme.util

import androidx.annotation.ColorInt
import androidx.annotation.Size
import androidx.core.graphics.ColorUtils

/**
 * Parse the color string, and return the corresponding color-int.
 * If the string cannot be parsed, throws an IllegalArgumentException
 * exception. Supported formats are:
 *
 * * <code>#RRGGBB</code>
 * * <code>#AARRGGBB</code>
 */
@ColorInt
fun parseColor(@Size(min = 1) colorString: String): Int {
    if (colorString[0] == '#') {
        // Use a long to avoid rollovers on #ffXXXXXX
        var color = colorString.substring(1).toLong(16)
        if (colorString.length == 7) {
            // Set the alpha value
            color = color or -0x1000000
        } else require(colorString.length == 9) { "Unknown color" }
        return color.toInt()
    }
    throw IllegalArgumentException("Unknown color")
}

/**
 * Parse the color string, and return the corresponding color-long.
 * If the string cannot be parsed, throws an IllegalArgumentException
 * exception. Supported formats are:
 *
 * * <code>#RRGGBB</code>
 * * <code>#AARRGGBB</code>
 */
fun parseColorL(@Size(min = 1) colorString: String): Long {
    if (colorString[0] == '#') {
        // Use a long to avoid rollovers on #ffXXXXXX
        var color = colorString.substring(1).toLong(16)
        if (colorString.length == 7) {
            // Set the alpha value
            color = color or 0xff000000
        } else require(colorString.length == 9) { "Unknown color" }
        return color
    }
    throw IllegalArgumentException("Unknown color")
}

/**
 * Turns e.g 0xffa5bef4 into "#ffa5bef4".
 */
fun encodeColor(color: Long): String {
    return "#${java.lang.Long.toHexString(color)}"
}

/**
 * Darken a color by a [factor] which is a value in the range 0..1f
 */
fun darkenColor(color: Int, factor: Float): Int {
    val x = FloatArray(3)
    ColorUtils.colorToHSL(color, x)
    x[2] = x[2] * (1 - factor.coerceIn(0f..1f))

    return ColorUtils.HSLToColor(x)
}