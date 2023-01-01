package com.peterlaurence.trekme.util

import androidx.annotation.ColorInt
import androidx.annotation.Size

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