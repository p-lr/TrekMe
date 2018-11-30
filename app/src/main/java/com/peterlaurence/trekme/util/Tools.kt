package com.peterlaurence.trekme.util

import java.io.PrintWriter
import java.io.StringWriter

/**
 * Convert a [Throwable]'s stacktrace to a String.
 */
fun stackTraceToString(t: Throwable): String {
    val errors = StringWriter()
    t.printStackTrace(PrintWriter(errors))
    return errors.toString()
}
