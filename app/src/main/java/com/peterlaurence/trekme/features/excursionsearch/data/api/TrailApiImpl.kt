package com.peterlaurence.trekme.features.excursionsearch.data.api

import com.peterlaurence.trekme.core.excursion.domain.model.OsmTrailGroup
import com.peterlaurence.trekme.core.excursion.domain.model.TrailDetail
import com.peterlaurence.trekme.core.excursion.domain.model.TrailSearchItem
import com.peterlaurence.trekme.core.map.domain.models.BoundingBoxNormalized
import com.peterlaurence.trekme.core.wmts.domain.model.X0
import com.peterlaurence.trekme.core.wmts.domain.model.X1
import com.peterlaurence.trekme.core.wmts.domain.model.Y0
import com.peterlaurence.trekme.core.wmts.domain.model.Y1
import com.peterlaurence.trekme.features.excursionsearch.domain.model.TrailApi
import com.peterlaurence.trekme.util.performRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class TrailApiImpl(private val httpClient: OkHttpClient, ): TrailApi {
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }
    private val baseUrl = "$waymarkedTrails/api/v1"

    override suspend fun search(boundingBox: BoundingBoxNormalized): List<TrailSearchItem> {
        val bbStr = makeBoundingBox(boundingBox)

        val request = Request.Builder()
            .url("$baseUrl/list/by_area?bbox=$bbStr&limit=20")
            .addHeader("Referer", waymarkedTrails)
            .addHeader("User-Agent", userAgent)
            .get()
            .build()

        return (httpClient.performRequest<ByAreaResponse>(request, json)?.results ?: emptyList()).map {
            it.toDomain()
        }
    }

    override suspend fun getDetails(boundingBox: BoundingBoxNormalized, ids: List<String>): List<TrailDetail> {
        val bbStr = makeBoundingBox(boundingBox)

        val request = Request.Builder()
            .url("$baseUrl/list/segments?bbox=$bbStr&relations=${ids.joinToString(separator = ",")}".also { println("xxxxx request $it") })
            .addHeader("Referer", waymarkedTrails)
            .addHeader("User-Agent", userAgent)
            .get()
            .build()

        val response = httpClient.performRequest<DetailResponse>(request, json)

        return response?.features?.map {
            TrailDetailImpl(it)
        } ?: emptyList()
    }

    private fun makeBoundingBox(boundingBox: BoundingBoxNormalized): String {
        fun deNormalize(t: Double, min: Double, max: Double): Double {
            return min + t * (max - min)
        }
        val xLeft = deNormalize(boundingBox.xLeft, X0, X1)
        val xRight = deNormalize(boundingBox.xRight, X0, X1)
        val yTop = deNormalize(boundingBox.yTop, Y0, Y1)
        val yBottom = deNormalize(boundingBox.yBottom, Y0, Y1)
        return "$xLeft,$yBottom,$xRight,$yTop"
    }
}

@Serializable
private data class ByAreaResponse(val results: List<SearchItem>)

@Serializable
private data class SearchItem(
    val id: String,
    val ref: String? = null,
    val name: String? = null,
    val group: String
)

@Serializable
private data class DetailResponse(val features: List<RelationDetail>)

@Serializable
private data class RelationDetail(val id: String, val geometry: Geom)

@Serializable
sealed interface Geom

@Serializable
@SerialName("MultiLineString")
private data class MultipleSegmentGeom(
    @SerialName("coordinates")
    val segments: List<Segment>
): Geom

@Serializable
@SerialName("LineString")
private data class SingleSegmentGeom(
    @SerialName("coordinates")
    val segment: Segment
): Geom

private typealias Segment = List<List<Double>>

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

private class TrailDetailImpl(
    private val feature: RelationDetail
): TrailDetail {

    override val id: String
        get() = feature.id

    override fun iteratePoints(block: (index: Int, x: Double, y: Double) -> Unit) {
        when (val geom = feature.geometry) {
            is MultipleSegmentGeom -> {
                for (segment in geom.segments.withIndex()) {
                    for (pt in segment.value) {
                        block(
                            segment.index,
                            normalize(pt[0], X0, X1),
                            normalize(pt[1], Y0, Y1)
                        )
                    }
                }
            }
            is SingleSegmentGeom -> {
                for (pt in geom.segment) {
                    block(0,
                        normalize(pt[0], X0, X1),
                        normalize(pt[1], Y0, Y1)
                    )
                }
            }
        }
    }

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }
}

private const val waymarkedTrails = "https://hiking.waymarkedtrails.org"
private const val userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"