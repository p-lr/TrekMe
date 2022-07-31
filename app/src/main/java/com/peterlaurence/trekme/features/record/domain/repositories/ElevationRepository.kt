package com.peterlaurence.trekme.features.record.domain.repositories

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.geotools.deltaTwoPoints
import com.peterlaurence.trekme.core.track.distanceCalculatorFactory
import com.peterlaurence.trekme.core.repositories.api.IgnApiRepository
import com.peterlaurence.trekme.util.chunk
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.features.common.domain.interactors.georecord.getElevationSource
import com.peterlaurence.trekme.features.common.domain.interactors.georecord.hasTrustedElevations
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource
import com.peterlaurence.trekme.util.performRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Generates elevation graph data, as state of the exposed [elevationRepoState]. Possible states are:
 * * [Calculating] indicating that a computation is ongoing
 * * [ElevationData] which contains corrected elevation data
 *
 * Some error events can be fired, such as:
 * * [ElevationCorrectionErrorEvent] indicating that the last computation couldn't be completed successfully
 * * [NoNetworkEvent] when it detects that remote servers aren't reachable
 *
 * Client code uses the [update] method to trigger either a graph data generation or an update.
 *
 * @since 2020/12/13
 */
class ElevationRepository(
    private val dispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val ignApiRepository: IgnApiRepository
) {
    private val _elevationRepoState = MutableStateFlow<ElevationState>(Calculating)
    val elevationRepoState: StateFlow<ElevationState> = _elevationRepoState.asStateFlow()

    private val _events = MutableSharedFlow<ElevationEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    private var lastId: UUID? = null
    private var job: Job? = null
    private val sampling = 20

    private val primaryScope = ProcessLifecycleOwner.get().lifecycleScope
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .cache(null)
        .build()

    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    /**
     * Computes elevation data for the given [GeoRecord] and updates the exposed
     * [elevationRepoState].
     */
    fun update(geoRecord: GeoRecord) {
        if (geoRecord.id != lastId) {
            job?.cancel()
            job = primaryScope.launch {
                _elevationRepoState.emit(Calculating)
                val (segmentElevationsList, eleSource, needsUpdate) = getElevationsSampled(geoRecord)
                val data =
                    makeElevationData(geoRecord, segmentElevationsList, eleSource, needsUpdate)
                _elevationRepoState.emit(data)
            }

            /* Avoid keeping reference on data */
            job?.invokeOnCompletion {
                job = null
            }
            lastId = geoRecord.id
        }
    }

    fun reset() = primaryScope.launch {
        _elevationRepoState.emit(Calculating)
    }

    /**
     * If the gpx doesn't have trusted elevations, try to get sub-sampled real elevations. Otherwise,
     * or if any error happens while fetching real elevations, fallback to just sub-sampling existing
     * elevations.
     * The sub-sampling is done by taking a point every [sampling] meters.
     */
    private suspend fun getElevationsSampled(
        geoRecord: GeoRecord
    ): TrackElevationsSubSampled = withContext(dispatcher) {
        /* We'll work on the first track only */
        val firstRouteGroup = geoRecord.routeGroups.firstOrNull() ?: return@withContext TrackElevationsSubSampled(
            listOf(), ElevationSource.GPS, false
        )
        val trustedElevations = geoRecord.hasTrustedElevations()

        suspend fun sampleWithoutApi(): List<SegmentElevationsSubSampled> {
            return firstRouteGroup.routes.map { route ->
                SegmentElevationsSubSampled(subSamplePoints(route.routeMarkers).toList())
            }
        }

        val noError = AtomicBoolean(true)
        val segmentElevations = if (!trustedElevations) {
            val segmentElevations = mutableListOf<SegmentElevationsSubSampled>()
            for (route in firstRouteGroup.routes) {
                val points = getRealElevationsForSegment(route)
                if (points == null) {
                    /* If something went wrong for one segment, don't process other segments */
                    noError.set(false)
                    break
                }

                segmentElevations.add(SegmentElevationsSubSampled(points))
            }
            if (noError.get()) segmentElevations else sampleWithoutApi()
        } else sampleWithoutApi()

        /* Needs update if it wasn't already trusted and there was no errors */
        val needsUpdate = !trustedElevations && noError.get()

        val eleSource = if (needsUpdate) ElevationSource.IGN_RGE_ALTI else geoRecord.getElevationSource()

        TrackElevationsSubSampled(segmentElevations, eleSource, needsUpdate)
    }

    /**
     * Sub-samples and fetch real elevations for a track segment. In case of any error, returns null.
     */
    private suspend fun getRealElevationsForSegment(route: Route): List<PointIndexed>? {
        return withContext(dispatcher) {
            val pointsFlow = subSamplePoints(route.routeMarkers)

            val noError = AtomicBoolean(true)

            suspend fun useApi() = runCatching {
                pointsFlow.chunk(40).buffer(8).flowOn(dispatcher).map { pts ->
                    flow {
                        /* If we fail to fetch elevation for one chunk, stop the whole flow */
                        val points = when (val eleResult = getElevations(
                            pts.map { it.lat }, pts.map { it.lon })
                        ) {
                            Error -> {
                                val apiStatus = checkElevationRestApi()
                                if (!apiStatus.internetOk || !apiStatus.restApiOk) {
                                    _events.tryEmit(
                                        NoNetworkEvent(
                                            apiStatus.internetOk,
                                            apiStatus.restApiOk
                                        )
                                    )
                                }
                                /* Stop the flow */
                                throw CancellationException()
                            }
                            NonTrusted -> {
                                noError.set(false)
                                pts
                            }
                            is TrustedElevations -> eleResult.elevations.zip(pts).map { (ele, pt) ->
                                PointIndexed(pt.index, pt.lat, pt.lon, ele)
                            }
                        }
                        emit(points)
                    }
                }.flattenMerge(8).flowOn(ioDispatcher).toList().flatten()
            }.getOrNull()

            useApi().takeIf { noError.get() }
        }
    }

    private suspend fun subSamplePoints(points: List<Marker>): Flow<PointIndexed> {
        return flow {
            var previousPt: Marker? = null
            points.mapIndexed { index, pt ->
                previousPt?.also { prev ->
                    val d = deltaTwoPoints(prev.lat, prev.lon, pt.lat, pt.lon)
                    if (d > sampling || index == points.lastIndex) {
                        emit(PointIndexed(index, pt.lat, pt.lon, pt.elevation ?: 0.0))
                    }
                } ?: suspend {
                    previousPt = pt
                    emit(PointIndexed(0, pt.lat, pt.lon, pt.elevation ?: 0.0))
                }()
            }
        }
    }

    private fun makeElevationData(
        geoRecord: GeoRecord,
        segmentElevationList: List<SegmentElevationsSubSampled>,
        eleSource: ElevationSource,
        needsUpdate: Boolean
    ): ElevationState {
        val firstRouteGroup = geoRecord.routeGroups.firstOrNull()
        if (firstRouteGroup == null || segmentElevationList.isEmpty()) return NoElevationData

        var distanceOffset = 0.0
        val segmentElePoints = firstRouteGroup.routes.zip(segmentElevationList)
            .map { (route, segmentEleSubSampled) ->
                val elePoints = interpolateSegment(geoRecord.hasTrustedElevations(), segmentEleSubSampled, route, distanceOffset)
                elePoints.lastOrNull()?.also {
                    distanceOffset = it.dist
                }
                SegmentElePoints(elePoints)
            }

        val subSampledPoints = segmentElevationList.flatMap { it.points }
        val minEle = subSampledPoints.minByOrNull { it.ele }?.ele ?: 0.0
        val maxEle = subSampledPoints.maxByOrNull { it.ele }?.ele ?: 0.0

        return ElevationData(geoRecord, segmentElePoints, minEle, maxEle, eleSource, needsUpdate, sampling)
    }

    private fun interpolateSegment(
        hasTrustedElevations: Boolean,
        segmentElevation: SegmentElevationsSubSampled,
        route: Route,
        distanceOffset: Double = 0.0
    ): List<ElePoint> {
        /* Take into account the trivial case where there is one or no points */
        if (segmentElevation.points.size < 2) {
            return segmentElevation.points.map { ElePoint(distanceOffset, it.ele) }
        }
        val ptsSorted = segmentElevation.points.sortedBy { it.index }.iterator()

        var previousRefPt = ptsSorted.next()
        var nextRefPt = ptsSorted.next()

        val distanceCalculator = distanceCalculatorFactory(hasTrustedElevations)

        return route.routeMarkers.mapIndexed { index, pt ->
            val ratio =
                (index - previousRefPt.index).toFloat() / (nextRefPt.index - previousRefPt.index)
            val ele = previousRefPt.ele + ratio * (nextRefPt.ele - previousRefPt.ele)
            distanceCalculator.addPoint(pt.lat, pt.lon, ele)

            if (index >= nextRefPt.index && ptsSorted.hasNext()) {
                previousRefPt = nextRefPt
                nextRefPt = ptsSorted.next()
            }

            ElePoint(dist = distanceOffset + distanceCalculator.getDistance(), ele)
        }
    }

    /**
     * Determine if we have an internet connection, then check the availability of the elevation
     * REST api.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun checkElevationRestApi(): ApiStatus = withContext(ioDispatcher) {
        val internetOk = runCatching {
            val ip = InetAddress.getByName("google.com")
            ip.hostAddress != ""
        }

        return@withContext if (internetOk.isSuccess) {
            val apiOk = runCatching {
                val apiIp = InetAddress.getByName(elevationServiceHost)
                apiIp.hostAddress != ""
            }
            ApiStatus(true, apiOk.getOrDefault(false))
        } else {
            ApiStatus(internetOk = false, restApiOk = false)
        }
    }

    /**
     * TODO: move this into data layer
     */
    private suspend fun getElevations(
        latList: List<Double>,
        lonList: List<Double>
    ): ElevationResult {
        val ignApi = ignApiRepository.getApi() ?: return Error
        val longitudeList = lonList.joinToString(separator = "|") { it.toString() }
        val latitudeList = latList.joinToString(separator = "|") { it.toString() }
        val url =
            "https://$elevationServiceHost/$ignApi/alti/rest/elevation.json?lon=$longitudeList&lat=$latitudeList"
        val req = ignApiRepository.requestBuilder.url(url).build()

        val eleList = withTimeoutOrNull(4000) {
            client.performRequest<ElevationsResponse>(req, json)?.elevations?.map { it.z }
        } ?: return Error

        return if (eleList.contains(-99999.0)) {
            NonTrusted
        } else TrustedElevations(eleList)
    }

    @Serializable
    private data class ElevationsResponse(val elevations: List<EleIgnPt>)

    @Serializable
    private data class EleIgnPt(val lat: Double, val lon: Double, val z: Double, val acc: Double)

    private data class ApiStatus(val internetOk: Boolean = false, val restApiOk: Boolean = false)

    private data class SegmentElevationsSubSampled(val points: List<PointIndexed>)

    private data class PointIndexed(
        val index: Int,
        val lat: Double,
        val lon: Double,
        val ele: Double
    )

    private data class TrackElevationsSubSampled(
        val segmentElevations: List<SegmentElevationsSubSampled>,
        val elevationSource: ElevationSource,
        val needsUpdate: Boolean
    )
}

private const val elevationServiceHost = "wxs.ign.fr"

sealed interface ElevationState
object Calculating : ElevationState
data class ElevationData(
    val geoRecord: GeoRecord,
    val segmentElePoints: List<SegmentElePoints> = listOf(),
    val eleMin: Double = 0.0,
    val eleMax: Double = 0.0,
    val elevationSource: ElevationSource,
    val needsUpdate: Boolean,
    val sampling: Int
) : ElevationState
object NoElevationData : ElevationState

sealed class ElevationEvent
data class NoNetworkEvent(val internetOk: Boolean, val restApiOk: Boolean) : ElevationEvent()
object ElevationCorrectionErrorEvent : ElevationEvent()

/**
 * A point representing the elevation at a given distance from the departure.
 *
 * @param dist distance in meters
 * @param elevation altitude in meters
 */
data class ElePoint(val dist: Double, val elevation: Double)

data class SegmentElePoints(val points: List<ElePoint>)

private sealed class ElevationResult
private object Error : ElevationResult()
private object NonTrusted : ElevationResult()
private data class TrustedElevations(val elevations: List<Double>) : ElevationResult()