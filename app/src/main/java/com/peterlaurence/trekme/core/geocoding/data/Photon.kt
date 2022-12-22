package com.peterlaurence.trekme.core.geocoding.data

import com.peterlaurence.trekme.core.geocoding.domain.model.GeocodingBackend
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.core.geocoding.domain.engine.POI
import com.peterlaurence.trekme.util.performRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import com.peterlaurence.trekme.core.geocoding.domain.engine.City as GeoCity
import com.peterlaurence.trekme.core.geocoding.domain.engine.Street as GeoStreet

/**
 * A [GeocodingBackend] which uses Komoot's Photon.
 *
 * @author P.Laurence on 01/01/21
 */
class Photon(private val client: OkHttpClient, private val json: Json) : GeocodingBackend {
    private val requestBuilder = Request.Builder()

    override suspend fun search(query: String): List<GeoPlace>? {
        val req = makeRequest(query)
        val resp = client.performRequest<PhotonMainResponse>(req, json)

        return resp?.let { convert(it) }
    }

    private fun makeRequest(query: String): Request {
        return requestBuilder.url("$photonApi?q=$query&limit=10").build()
    }

    private fun convert(response: PhotonMainResponse): List<GeoPlace>? {
        if (response.features.isEmpty()) return null
        return response.features.mapNotNull {
            if (it.geometry.coordinates.size == 2) {
                val lon = it.geometry.coordinates[0]
                val lat = it.geometry.coordinates[1]
                val geoType = when (it.properties.type) {
                    "street" -> GeoStreet
                    "city" -> GeoCity
                    "house" -> POI
                    else -> POI
                }
                val name = it.properties.name
                val locality = listOfNotNull(it.properties.city, it.properties.postcode, it.properties.state).joinToString(", ")
                GeoPlace(geoType, name, locality, lat, lon)
            } else null
        }
    }
}

private const val photonApi = "https://photon.komoot.io/api/"

@Serializable
private data class PhotonMainResponse(val features: List<PhotonFeature>)

@Serializable
private data class PhotonFeature(val geometry: PhotonGeometry, val properties: PhotonProperties)

@Serializable
private data class PhotonGeometry(val coordinates: List<Double>)

@Serializable
private data class PhotonProperties(val name: String, val city: String? = null, val country: String, val postcode: String? = null, val state: String, val type: String)
