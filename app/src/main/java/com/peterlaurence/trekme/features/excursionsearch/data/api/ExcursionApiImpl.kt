package com.peterlaurence.trekme.features.excursionsearch.data.api

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionCategory
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionSearchItem
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.features.excursionsearch.domain.model.ExcursionApi
import com.peterlaurence.trekme.features.excursionsearch.presentation.model.ExcursionCategoryChoice
import com.peterlaurence.trekme.util.performRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class ExcursionApiImpl(private val httpClient: OkHttpClient) : ExcursionApi {
    private val requestBuilder = Request.Builder()
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }
    private val baseUrl = "http://192.168.1.18:8080/excursion"

    override suspend fun search(
        lat: Double,
        lon: Double,
        categoryChoice: ExcursionCategoryChoice
    ): List<ExcursionSearchItem> {
        val request = makeRequest(lat, lon, categoryChoice)
        return httpClient.performRequest<List<Ref>>(request, json)?.map { it.toDomain() } ?: emptyList()
    }

    private fun makeRequest(lat: Double, lon: Double, categoryChoice: ExcursionCategoryChoice): Request {
        return when(categoryChoice) {
            ExcursionCategoryChoice.All -> {
                requestBuilder.url("$baseUrl/search?lat=$lat&lon=$lon").build()
            }
            is ExcursionCategoryChoice.Single -> {
                requestBuilder.url("$baseUrl/search?lat=$lat&lon=$lon&category=${categoryChoice.choice.toApiString()}").build()
            }
        }
    }

    private fun ExcursionCategory.toApiString(): String {
        return when(this) {
            ExcursionCategory.OnFoot -> "on-foot"
            ExcursionCategory.Bike -> "bike"
            ExcursionCategory.Horse -> "horse"
            ExcursionCategory.Nautical -> "nautical"
            ExcursionCategory.Aerial -> "aerial"
            ExcursionCategory.Motorised -> "motorized"
        }
    }
}

@Serializable
private data class Ref(
    val id: String,
    val title: String,
    val type: String,
    val description: String = ""
)

private fun Ref.toDomain(): ExcursionSearchItem  {
    val typeDomain = when(type) {
        "hike" -> ExcursionType.Hike
        "running" -> ExcursionType.Running
        "mountain-bike" -> ExcursionType.MountainBike
        "travel-bike" -> ExcursionType.TravelBike
        "horse-riding" -> ExcursionType.HorseRiding
        else -> ExcursionType.Hike
    }
    return ExcursionSearchItem(id = id, title = title, type = typeDomain, description = description)
}