@file:Suppress("LocalVariableName")

package com.peterlaurence.trekme.repositories.recording

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.core.geotools.deltaTwoPoints
import com.peterlaurence.trekme.repositories.ign.IgnApiRepository
import com.peterlaurence.trekme.util.chunk
import com.peterlaurence.trekme.util.gpx.model.Gpx
import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import com.peterlaurence.trekme.util.performRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Generates elevation graph data, as state of the exposed [elevationRepoState]. Other states are:
 * * [Calculating] indicating that a computation is ongoing
 * * [ElevationCorrectionError] indicating that the last computation couldn't be completed successfully
 * * [NoNetwork] when it detects that remote servers aren't reachable
 *
 * Client code uses the [update] method to trigger either a graph data generation or an update.
 *
 * @author P.Laurence on 13/12/20
 */
class ElevationRepository(
        private val dispatcher: CoroutineDispatcher,
        private val ioDispatcher: CoroutineDispatcher,
        private val ignApiRepository: IgnApiRepository
) {
    private val _elevationRepoState = MutableStateFlow<ElevationState>(Calculating)
    val elevationRepoState: StateFlow<ElevationState> = _elevationRepoState.asStateFlow()

    private var lastGpxId: Int? = null
    private var lastTargetWidth: Int? = null
    private var job: Job? = null

    private val primaryScope = ProcessLifecycleOwner.get().lifecycleScope
    private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cache(null)
            .build()

    /**
     * Computes elevation data from the given [GpxForElevation] and [targetWidth], and updates the
     * exposed [elevationRepoState].
     * When [gpxData] is null, the state is set to [Calculating]. Otherwise, the id of [gpxData] is
     * used to decide whether to cancel the ongoing work or not.
     *
     * @param gpxData The data to work on.
     * @param targetWidth The actual amount of pixels in horizontal dimension. If the tracks has too
     * many points, it will be sub-sampled.
     */
    fun update(gpxData: GpxForElevation?, targetWidth: Int) {
        if (gpxData == null) {
            primaryScope.launch {
                _elevationRepoState.emit(Calculating)
            }
            return
        }
        val (gpx, id) = gpxData
        if (id != lastGpxId || targetWidth != lastTargetWidth
                || _elevationRepoState.value is NoNetwork
                || _elevationRepoState.value is ElevationCorrectionError) {
            job?.cancel()
            job = primaryScope.launch {
                _elevationRepoState.emit(Calculating)

                val apiStatus = checkElevationRestApi()
                if (apiStatus.restApiOk && apiStatus.restApiOk) {
                    val realElevations = getRealElevations(gpx)

                    if (realElevations == null) {
                        _elevationRepoState.emit(ElevationCorrectionError)
                        return@launch
                    }

                    val data = makeElevationData(gpx, realElevations).let {
                        /* Sub-sample the result */
                        if (it is ElevationData) {
                            it.copy(points = it.points.subSample(targetWidth))
                        } else it
                    }
                    _elevationRepoState.emit(data)
                } else {
                    _elevationRepoState.emit(NoNetwork(apiStatus.restApiOk))
                }
            }

            /* Avoid keeping reference on data */
            job?.invokeOnCompletion {
                job = null
            }
            lastTargetWidth = targetWidth
            lastGpxId = id
        }
    }

    /**
     * Get real elevations every 20m. Returns null when an error occurred which would make the returned
     * list inconsistent (we shall not mix real elevations with original elevations from the GPS).
     */
    private suspend fun getRealElevations(gpx: Gpx): List<PointIndexed>? = withContext(dispatcher) {
        val pointsFlow = flow {
            var previousPt: TrackPoint? = null
            val trackPoints = gpx.tracks.firstOrNull()?.trackSegments?.firstOrNull()?.trackPoints
            trackPoints?.mapIndexed { index, pt ->
                previousPt?.also { prev ->
                    val d = deltaTwoPoints(prev.latitude, prev.longitude, pt.latitude, pt.longitude)
                    if (d > 20 || index == trackPoints.lastIndex) {
                        emit(PointIndexed(index, pt.latitude, pt.longitude, pt.elevation ?: 0.0))
                    }
                } ?: suspend {
                    previousPt = pt
                    emit(PointIndexed(0, pt.latitude, pt.longitude, pt.elevation ?: 0.0))
                }()
            }
        }

        runCatching {
            pointsFlow.chunk(40).buffer(8).flowOn(dispatcher).map { pts ->
                flow {
                    /* If we fail to fetch elevation for one chunk, stop the whole flow */
                    val points = when (val eleResult = getElevations(
                            pts.map { it.lat }, pts.map { it.lon })
                    ) {
                        Error -> throw Exception("Missing data")
                        NonTrusted -> pts
                        is TrustedElevations -> eleResult.elevations.zip(pts).map { (ele, pt) ->
                            PointIndexed(pt.index, pt.lat, pt.lon, ele)
                        }
                    }
                    emit(points)
                }
            }.flattenMerge(8).flowOn(ioDispatcher).toList().flatten()
        }.getOrNull()
    }

    /**
     * Perform interpolation between each real elevations.
     */
    private fun makeElevationData(gpx: Gpx, points: List<PointIndexed>): ElevationState {
        /* Take into account the trivial case where there is one or no points */
        if (points.size < 2) {
            val ele = points.firstOrNull()?.ele ?: 0.0
            return ElevationData(points.map { ElePoint(0.0, it.ele) }, ele, ele)
        }
        val ptsSorted = points.sortedBy { it.index }.iterator()

        var dist = 0.0
        var previousRefPt = ptsSorted.next()
        var nextRefPt = ptsSorted.next()
        val interpolated = gpx.tracks.firstOrNull()?.trackSegments?.firstOrNull()?.trackPoints?.mapIndexed { index, pt ->
            val ratio = (index - previousRefPt.index).toFloat() / (nextRefPt.index - previousRefPt.index)
            val ele = previousRefPt.ele + ratio * (nextRefPt.ele - previousRefPt.ele)
            val distDelta = deltaTwoPoints(previousRefPt.lat, previousRefPt.lon, previousRefPt.ele, pt.latitude, pt.longitude, ele)

            if (index >= nextRefPt.index && ptsSorted.hasNext()) {
                previousRefPt = nextRefPt
                nextRefPt = ptsSorted.next()
            }
            dist += distDelta
            ElePoint(dist, ele)
        }

        val minEle = points.minByOrNull { it.ele }?.ele ?: 0.0
        val maxEle = points.maxByOrNull { it.ele }?.ele ?: 0.0

        return ElevationData(interpolated ?: listOf(), minEle, maxEle)
    }

    /**
     * Sub-sample if the list size exceeds 2 * [targetWidth].
     */
    private fun List<ElePoint>.subSample(targetWidth: Int): List<ElePoint> {
        val chunkSize = size / targetWidth
        return if (chunkSize >= 2) {
            chunked(chunkSize).map { chunk ->
                val dist = chunk.sumByDouble { it.dist } / chunk.size
                val ele = chunk.sumByDouble { it.elevation } / chunk.size
                ElePoint(dist, ele)
            }
        } else this
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

    private suspend fun getElevations(latList: List<Double>, lonList: List<Double>): ElevationResult {
        val ignApi = ignApiRepository.getApi()
        val longitudeList = lonList.joinToString(separator = "|") { it.toString() }
        val latitudeList = latList.joinToString(separator = "|") { it.toString() }
        val url = "http://$elevationServiceHost/$ignApi/alti/rest/elevation.json?lon=$longitudeList&lat=$latitudeList"
        val req = ignApiRepository.requestBuilder.url(url).build()
        val eleList = client.performRequest<ElevationsResponse>(req)?.elevations?.map { it.z }
                ?: return Error
        return if (eleList.contains(-99999.0)) {
            NonTrusted
        } else TrustedElevations(eleList)
    }

    @Serializable
    private data class ElevationsResponse(val elevations: List<EleIgnPt>)

    @Serializable
    private data class EleIgnPt(val lat: Double, val lon: Double, val z: Double, val acc: Double)

    private data class ApiStatus(val internetOk: Boolean = false, val restApiOk: Boolean = false)

    private data class PointIndexed(val index: Int, val lat: Double, val lon: Double, val ele: Double)

}

private const val elevationServiceHost = "wxs.ign.fr"

sealed class ElevationState
object Calculating : ElevationState()
data class NoNetwork(val restApiOk: Boolean) : ElevationState()
object ElevationCorrectionError : ElevationState()
data class ElevationData(val points: List<ElePoint> = listOf(), val eleMin: Double = 0.0, val eleMax: Double = 0.0) : ElevationState()

/**
 * A point representing the elevation at a given distance from the departure.
 *
 * @param dist distance in meters
 * @param elevation altitude in meters
 */
data class ElePoint(val dist: Double, val elevation: Double)

private sealed class ElevationResult
private object Error : ElevationResult()
private object NonTrusted : ElevationResult()
private data class TrustedElevations(val elevations: List<Double>) : ElevationResult()