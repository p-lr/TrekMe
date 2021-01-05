package com.peterlaurence.trekme.core.geocoding.backend

import com.peterlaurence.trekme.core.geocoding.GeoPlace
import com.peterlaurence.trekme.core.geocoding.POI
import com.peterlaurence.trekme.core.geocoding.Street
import com.peterlaurence.trekme.util.performRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * A [GeocodingBackend] which uses Nominatim.
 * @see [Nominatim](https://github.com/osm-search/Nominatim)
 *
 * @author P.Laurence on 01/01/21
 */
class Nominatim(private val client: OkHttpClient) : GeocodingBackend {
    private val requestBuilder = Request.Builder()

    override suspend fun search(query: String): List<GeoPlace>? {
        val req = makeRequest(query)
        val resp = client.performRequest<List<NominatimJson>>(req)

        return resp?.let { convert(it) }
    }

    private fun makeRequest(query: String): Request {
        return requestBuilder.url("${nomonatimApi}search?q=$query&format=jsonv2").build()
    }

    private fun convert(response: List<NominatimJson>): List<GeoPlace>? {
        if (response.isEmpty()) return null
        return response.map {
            val type = if (it.type.contains("residential") || it.type.contains("street")) {
                 Street
            } else POI
            val name = it.displayName.substringBefore(",")
            val allInfos = it.displayName.split(',').map { it.trim() }

            val locality = if (allInfos.size > 4) {
                "${allInfos[2]}, ${allInfos[allInfos.size-2]}, ${allInfos[allInfos.size-1]}"
            } else it.displayName.substringAfter(",")

            GeoPlace(type, name, locality, it.lat, it.lon)
        }
    }
}

const val nomonatimApi = "https://nominatim.openstreetmap.org/"

@Serializable
private data class NominatimJson(
        @SerialName("display_name")
        val displayName: String,
        val lat: Double,
        val lon: Double,
        val type: String
)