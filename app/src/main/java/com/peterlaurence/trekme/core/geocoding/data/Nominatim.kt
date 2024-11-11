package com.peterlaurence.trekme.core.geocoding.data

import com.peterlaurence.trekme.core.geocoding.domain.model.GeocodingBackend
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.core.geocoding.domain.engine.POI
import com.peterlaurence.trekme.core.geocoding.domain.engine.Street
import com.peterlaurence.trekme.util.performRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * A [GeocodingBackend] which uses Nominatim.
 * @see [Nominatim](https://github.com/osm-search/Nominatim)
 *
 * @since 01/01/21
 */
class Nominatim(private val client: OkHttpClient) : GeocodingBackend {
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }
    private val requestBuilder = Request.Builder()

    override suspend fun search(query: String): List<GeoPlace>? {
        if (query.length <= 2) return emptyList()
        val req = makeRequest(query)
        val resp = client.performRequest<List<NominatimJson>>(req, json)

        return resp?.let { convert(it) }
    }

    private fun makeRequest(query: String): Request {
        return requestBuilder
            .url("${nominatimApi}search?q=$query&format=jsonv2")
            .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
            .header("Cache-Control", "public, max-age=604800") // 7 days
            .build()
    }

    private fun convert(response: List<NominatimJson>): List<GeoPlace>? {
        if (response.isEmpty()) return null
        return response.map {
            val type = if (it.type.contains("residential") || it.type.contains("street")) {
                Street
            } else POI

            val allInfos = it.displayName.split(',').map { it.trim() }
            val name = if (allInfos.size > 4) {
                allInfos.take(2).joinToString(separator = ", ")
            } else {
                it.displayName.substringBefore(",")
            }

            val locality = if (allInfos.size > 4) {
                "${allInfos[2]}, ${allInfos[allInfos.size - 2]}, ${allInfos[allInfos.size - 1]}"
            } else it.displayName.substringAfter(",")

            GeoPlace(type, name, locality, it.lat, it.lon)
        }
    }
}

const val nominatimApi = "https://nominatim.openstreetmap.org/"

@Serializable
private data class NominatimJson(
    @SerialName("display_name")
    val displayName: String,
    val lat: Double,
    val lon: Double,
    val type: String
)