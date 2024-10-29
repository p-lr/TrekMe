package com.peterlaurence.trekme.core.elevation.data.datasource

import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.contains
import com.peterlaurence.trekme.core.elevation.domain.datasource.ElevationDataSource
import com.peterlaurence.trekme.core.elevation.domain.model.ElevationResult
import com.peterlaurence.trekme.core.elevation.domain.model.Error
import com.peterlaurence.trekme.core.elevation.domain.model.NonTrusted
import com.peterlaurence.trekme.core.elevation.domain.model.TrustedElevations
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

    private val geopfHost = "data.geopf.fr"

    private val bounds = listOf(
        BoundingBox(42.33, 51.10, -5.15, 8.24),      // France
        BoundingBox(41.32, 43.03, 8.53, 9.57),       // Corse
        BoundingBox(15.82, 16.52, -61.82, -60.99),   // Guadeloupe
        BoundingBox(2.10, 5.76, -54.61, -51.61),     // Guyane
        BoundingBox(16.16, 16.35, -61.13, -61.00),   // La Désirade
        BoundingBox(15.83, 15.89, -61.66, -61.56),   // Les Saintes
        BoundingBox(15.86, 16.01, -61.34, -61.19),   // Marie-Galante
        BoundingBox(14.38, 14.89, -61.24, -60.80),   // Martinique
        BoundingBox(-13.02, -12.63, 45.01, 45.31),   // Mayotte
        BoundingBox(-21.40, -20.86, 55.21, 55.85),   // Réunion
        BoundingBox(17.87, 17.98, -62.93, -62.78),   // Saint-Barthélemy
        BoundingBox(18.04, 18.13, -63.16, -62.90),   // Saint-Martin
        BoundingBox(46.74, 47.15, -56.53, -56.07),   // St Pierre & Miq
    )

    override suspend fun getElevations(
        latList: List<Double>,
        lonList: List<Double>
    ): ElevationResult {
        val longitudeList = lonList.joinToString(separator = "|") { it.toString() }
        val latitudeList = latList.joinToString(separator = "|") { it.toString() }
        val url =
            "https://$geopfHost/altimetrie/1.0/calcul/alti/rest/elevation.json?lon=$longitudeList&lat=$latitudeList&resource=ign_rge_alti_wld&delimiter=|&indent=false&measures=false&zonly=true"
        val req = Request.Builder().url(url).build()

        val eleList = withTimeoutOrNull(4000) {
            client.performRequest<ElevationsResponse>(req, json)?.elevations
        } ?: return Error

        return if (eleList.contains(-99999.0)) {
            NonTrusted
        } else TrustedElevations(eleList)
    }

    /**
     * Determine if we have an internet connection, then check the availability of the elevation
     * REST api.
     */
    override suspend fun checkStatus(): Boolean = withContext(ioDispatcher) {
        runCatching {
            val apiIp = InetAddress.getByName(geopfHost)
            apiIp.hostAddress != ""
        }.isSuccess
    }

    override fun isInCoverage(lat: Double, lon: Double): Boolean {
        return bounds.contains(lat, lon)
    }

    @Serializable
    private data class ElevationsResponse(val elevations: List<Double>)
}