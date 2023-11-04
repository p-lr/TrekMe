package com.peterlaurence.trekme.features.excursionsearch.data.api

import com.peterlaurence.trekme.core.excursion.domain.model.OsmTrailGroup
import com.peterlaurence.trekme.core.excursion.domain.model.TrailSearchItem
import com.peterlaurence.trekme.core.map.domain.models.BoundingBoxNormalized
import com.peterlaurence.trekme.core.wmts.domain.model.X0
import com.peterlaurence.trekme.core.wmts.domain.model.X1
import com.peterlaurence.trekme.core.wmts.domain.model.Y0
import com.peterlaurence.trekme.core.wmts.domain.model.Y1
import com.peterlaurence.trekme.features.excursionsearch.domain.model.TrailApi
import com.peterlaurence.trekme.util.performRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class TrailApiImpl(private val httpClient: OkHttpClient, ): TrailApi {
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }
    private val baseUrl = "$waymarkedTrails/api/v1"

    override suspend fun search(boundingBox: BoundingBoxNormalized): List<TrailSearchItem> {
        fun deNormalize(t: Double, min: Double, max: Double): Double {
            return min + t * (max - min)
        }
        val xLeft = deNormalize(boundingBox.xLeft, X0, X1)
        val xRight = deNormalize(boundingBox.xRight, X0, X1)
        val yTop = deNormalize(boundingBox.yTop, Y0, Y1)
        val yBottom = deNormalize(boundingBox.yBottom, Y0, Y1)
        val bbStr = "$xLeft,$yBottom,$xRight,$yTop"
        val request = Request.Builder()
            .url("$baseUrl/list/by_area?bbox=$bbStr&limit=20")
            .addHeader("Referer", waymarkedTrails)
            .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
            .get()
            .build()

        return (httpClient.performRequest<ByAreaResponse>(request, json)?.results ?: emptyList()).map {
            it.toDomain()
        }
    }
}

@Serializable
private data class ByAreaResponse(val results: List<SearchItem>)

@Serializable
private data class SearchItem(
    val id: String,
    val ref: String? = null,
    val name: String,
    val group: String
)

private fun SearchItem.toDomain(): TrailSearchItem {
    return TrailSearchItem(
        id = id, ref = ref, name = name,
        group = when (group) {
            "INT" -> OsmTrailGroup.International
            "NAT" -> OsmTrailGroup.National
            "REG" -> OsmTrailGroup.Regional
            "LOC" -> OsmTrailGroup.Local
            else -> null
        }
    )
}

private const val waymarkedTrails = "https://hiking.waymarkedtrails.org"