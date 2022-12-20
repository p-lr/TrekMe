package com.peterlaurence.trekme.core.repositories.api

import com.peterlaurence.trekme.core.settings.backendApiServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URL

/**
 * Lazily fetch the API key for Ordnance Survey.
 */
class OrdnanceSurveyApiRepository {
    private var api: String? = null

    suspend fun getApi(): String? {
        if (api == null) {
            api = withTimeoutOrNull(3000) {
                queryApi(ordnanceSurveyApiUrl)
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

private const val ordnanceSurveyApiUrl = "$backendApiServer/ordnance-survey-api"