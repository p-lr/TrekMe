package com.peterlaurence.trekme.util

import java.io.File
import java.io.IOException
import java.io.PrintWriter

fun writeToFile(st: String, out: File, errCb: () -> Unit) {
    try {
        PrintWriter(out).use {
            it.print(st)
        }
    } catch (e: IOException) {
        errCb()
    }
}
