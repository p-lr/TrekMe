package com.peterlaurence.trekme.repositories.api

import com.peterlaurence.trekme.data.backendApi.backendApiServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import java.net.URL


/**
 * Lazily fetch the API key for France IGN
 */
class IgnApiRepository {
    private var api: String? = null

    val requestBuilder: Request.Builder
        get() = Request.Builder().header("User-Agent", ignUserAgent)

    suspend fun getApi(): String? {
        if (api == null) {
            api = withTimeoutOrNull(3000) {
                queryApi(ignApiUrl)
            }
        }
        return api
    }

    private suspend fun queryApi(urlStr: String): String? = withContext(Dispatchers.IO) {
        val url = URL(urlStr)
        val connection = url.openConnection()
        try {
            connection.getInputStream().bufferedReader().use {
                it.readText()
            }
        } catch (t: Throwable) {
            null
        }
    }
}

private const val ignApiUrl = "$backendApiServer/ign-api"
private const val ignUserAgent = "TrekMe"