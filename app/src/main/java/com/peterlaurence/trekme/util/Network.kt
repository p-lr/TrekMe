package com.peterlaurence.trekme.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

/**
 * Determine if we have an internet connection.
 */
suspend fun checkInternet(): Boolean = withContext(Dispatchers.IO) {
    try {
        val ip = InetAddress.getByName("google.com")
        ip.hostAddress != ""
    } catch (e: Throwable) {
        false
    }
}