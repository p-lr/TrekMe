package com.peterlaurence.trekme.features.trailsearch.data.api

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionCategory
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionSearchItem
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.settings.backendApiServer
import com.peterlaurence.trekme.features.trailsearch.domain.model.ExcursionApi
import com.peterlaurence.trekme.util.performRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream

class ExcursionApiImpl(
    private val httpClient: OkHttpClient,
    private val geoRecordParser: GeoRecordParser
) : ExcursionApi {
    private val requestBuilder = Request.Builder()
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }
    private val baseUrl = "$backendApiServer/excursion"
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun search(
        lat: Double,
        lon: Double,
        category: ExcursionCategory?
    ): List<ExcursionSearchItem> {
        val request = makeRequest(lat, lon, category)
        return httpClient.performRequest<List<Item>>(request, json)?.map { it.toDomain() }
            ?: emptyList()
    }

    override suspend fun getGeoRecord(id: String, name: String): GeoRecord? {
        val req = Request.Builder()
            .url("$baseUrl/gpx/$id")
            .get()
            .build()

        val gpx = httpClient.performRequest(req) ?: return null

        return geoRecordParser.parse(ByteArrayInputStream(gpx.toByteArray()), name)
    }

    private fun makeRequest(lat: Double, lon: Double, category: ExcursionCategory?): Request {
        val form = if (category == null) {
            SearchForm(lat, lon)
        } else {
            SearchForm(lat, lon, category = category.toApiString())
        }

        return requestBuilder
            .url("$baseUrl/search")
            .post(json.encodeToString(form).toRequestBody(mediaType))
            .build()
    }

    private fun ExcursionCategory.toApiString(): String {
        return when (this) {
            ExcursionCategory.OnFoot -> "on-foot"
            ExcursionCategory.Bike -> "bike"
            ExcursionCategory.Horse -> "horse"
            ExcursionCategory.Nautical -> "nautical"
            ExcursionCategory.Aerial -> "aerial"
            ExcursionCategory.Motorised -> "motorised"
        }
    }
}

@Serializable
private data class SearchForm(
    val lat: Double,
    val lon: Double,
    val radiusKm: Int? = null,
    val category: String? = null
)

@Serializable
private data class Item(
    val id: String,
    val title: String,
    val type: String,
    val startLat: Double,
    val startLon: Double,
    val description: String = ""
)

private fun Item.toDomain(): ExcursionSearchItem {
    val typeDomain = when (type) {
        "hike" -> ExcursionType.Hike
        "running" -> ExcursionType.Running
        "mountain-bike" -> ExcursionType.MountainBike
        "travel-bike" -> ExcursionType.TravelBike
        "horse-riding" -> ExcursionType.HorseRiding
        else -> ExcursionType.Hike
    }
    return ExcursionSearchItem(
        id = id,
        title = title,
        type = typeDomain,
        startLat = startLat,
        startLon = startLon,
        description = description
    )
}