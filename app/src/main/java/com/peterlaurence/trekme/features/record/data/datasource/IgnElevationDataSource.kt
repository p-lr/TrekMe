package com.peterlaurence.trekme.features.record.data.datasource

import com.peterlaurence.trekme.features.record.domain.datasource.ElevationDataSource
import com.peterlaurence.trekme.features.record.domain.datasource.model.*
import com.peterlaurence.trekme.util.performRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class IgnElevationDataSource(
    private val ioDispatcher: CoroutineDispatcher
): ElevationDataSource {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .cache(null)
        .build()

    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    private val elevationServiceHost = "wxs.ign.fr"

    override suspend fun getElevations(
        latList: List<Double>,
        lonList: List<Double>
    ): ElevationResult {
        val longitudeList = lonList.joinToString(separator = "|") { it.toString() }
        val latitudeList = latList.joinToString(separator = "|") { it.toString() }
        val url =
            "https://$elevationServiceHost/calcul/alti/rest/elevation.json?lon=$longitudeList&lat=$latitudeList"
        val req = Request.Builder().url(url).build()

        val eleList = withTimeoutOrNull(4000) {
            client.performRequest<ElevationsResponse>(req, json)?.elevations?.map { it.z }
        } ?: return Error

        return if (eleList.contains(-99999.0)) {
            NonTrusted
        } else TrustedElevations(eleList)
    }

    /**
     * Determine if we have an internet connection, then check the availability of the elevation
     * REST api.
     */
    override suspend fun checkStatus(): ApiStatus = withContext(ioDispatcher){
        val internetOk = runCatching {
            val ip = InetAddress.getByName("google.com")
            ip.hostAddress != ""
        }

        if (internetOk.isSuccess) {
            val apiOk = runCatching {
                val apiIp = InetAddress.getByName(elevationServiceHost)
                apiIp.hostAddress != ""
            }
            ApiStatus(true, apiOk.getOrDefault(false))
        } else {
            ApiStatus(internetOk = false, restApiOk = false)
        }
    }

    @Serializable
    private data class ElevationsResponse(val elevations: List<EleIgnPt>)

    @Serializable
    private data class EleIgnPt(val lat: Double, val lon: Double, val z: Double, val acc: Double)
}