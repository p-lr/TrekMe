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

fun Throwable.stackTraceAsString(): String {
    val sw = StringWriter()
    printStackTrace(PrintWriter(sw))
    return sw.toString()
}
