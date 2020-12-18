@file:Suppress("LocalVariableName")

package com.peterlaurence.trekme.repositories.recording

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.core.geotools.deltaTwoPoints
import com.peterlaurence.trekme.repositories.ign.IgnApiRepository
import com.peterlaurence.trekme.util.gpx.model.Gpx
import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import com.peterlaurence.trekme.util.performRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import java.net.InetAddress

class ElevationRepository(
        private val dispatcher: CoroutineDispatcher,
        private val ioDispatcher: CoroutineDispatcher,
        private val gpxRepository: GpxRepository,
        private val ignApiRepository: IgnApiRepository
) {
    private val _elevationRepoState = MutableStateFlow<ElevationState>(Calculating)
    val elevationRepoState: StateFlow<ElevationState> = _elevationRepoState.asStateFlow()

    private var lastGpxId: Int? = null
    private var lastTargetWidth: Int? = null
    private var job: Job? = null

    /**
     * Retrieves the latest [GpxForElevation] from the [GpxRepository], and uses the id to decide
     * whether to cancel the ongoing work or not.
     *
     * @param targetWidth The actual amount of pixels in horizontal dimension. If the tracks has too
     * many points, it will be sub-sampled.
     */
    fun update(targetWidth: Int) {
        val gpxData: GpxForElevation = gpxRepository.gpxForElevation ?: return
        val (gpx, id) = gpxData
        if (id != lastGpxId || targetWidth != lastTargetWidth || _elevationRepoState.value is NoNetwork) {
            job?.cancel()
            job = ProcessLifecycleOwner.get().lifecycleScope.launch {
                val apiStatus = checkElevationRestApi()
                if (!apiStatus.internetOk || !apiStatus.restApiOk) {
                    _elevationRepoState.emit(NoNetwork)
                    return@launch
                }
                _elevationRepoState.emit(Calculating)
                val data = gpxToElevationData(gpx, targetWidth)
                _elevationRepoState.emit(data)
            }

            /* Avoid keeping reference on data */
            job?.invokeOnCompletion {
                job = null
            }
            lastTargetWidth = targetWidth
            lastGpxId = id
        }
    }

    private suspend fun gpxToElevationData(gpx: Gpx, targetWidth: Int): ElevationData = withContext(dispatcher) {
        var dist = 0.0
        var lastPt: TrackPoint? = null
        var minElePt: TrackPoint? = null
        var maxElePt: TrackPoint? = null
        val points = gpx.tracks.firstOrNull()?.trackSegments?.firstOrNull()?.trackPoints?.mapNotNull { pt ->
            val lastPt_ = lastPt
            val lastEle = lastPt_?.elevation
            val newEle = pt.elevation
            if (lastEle != null && newEle != null) {
                dist += deltaTwoPoints(lastPt_.latitude, lastPt_.longitude, lastEle,
                        pt.latitude, pt.longitude, newEle)
            }
            if (newEle != null) {
                if (minElePt == null) minElePt = pt
                minElePt?.also {
                    val curMin = it.elevation
                    if (curMin != null && newEle < curMin) minElePt = pt
                }
                if (maxElePt == null) maxElePt = pt
                maxElePt?.also {
                    val curMax = it.elevation
                    if (curMax != null && newEle > curMax) maxElePt = pt
                }
            }
            lastPt = pt
            if (newEle != null) ElePoint(dist, newEle) else null
        }

        val minEle_ = minElePt?.elevation
        val maxEle_ = maxElePt?.elevation

        if (points != null && minEle_ != null && maxEle_ != null) {
            val cor = computeCorrection(minElePt, minEle_, maxElePt, maxEle_)
            val subSampled = points.subSample(targetWidth)
            val corrected = subSampled.map {
                it.copy(elevation = it.elevation + cor)
            }
            ElevationData(corrected, minEle_ + cor, maxEle_ + cor)
        } else {
            ElevationData(listOf(), 0.0, 0.0)
        }
    }

    private suspend fun computeCorrection(pt1: TrackPoint?, ele1: Double, pt2: TrackPoint?, ele2: Double): Double {
        return if (pt1 != null && pt2 != null) {
            val realElevations = getElevations(listOf(pt1, pt2))
            if (realElevations != null && realElevations.isNotEmpty()) {
                realElevations.zip(listOf(ele1, ele2)).let {
                    it.sumByDouble { p ->
                        (p.first - p.second)
                    } / it.size
                }
            } else 0.0
        } else 0.0
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
                val apiIp = InetAddress.getByName("wxs.ign.fr")
                apiIp.hostAddress != ""
            }
            ApiStatus(true, apiOk.getOrDefault(false))
        } else {
            ApiStatus(internetOk = false, restApiOk = false)
        }
    }

    private suspend fun getElevations(trkPoints: List<TrackPoint>): List<Double>? {
        val client = OkHttpClient()
        val ignApi = ignApiRepository.getApi()
        val longitudeList = trkPoints.joinToString(separator = "|") { "${it.longitude}" }
        val latitudeList = trkPoints.joinToString(separator = "|") { "${it.latitude}" }
        val url = "http://wxs.ign.fr/$ignApi/alti/rest/elevation.json?lon=$longitudeList&lat=$latitudeList&zonly=true"
        val req = ignApiRepository.requestBuilder.url(url).build()
        return performRequest<ElevationsResponse>(client, req)?.elevations
    }

    @Serializable
    private data class ElevationsResponse(val elevations: List<Double>)

    private data class ApiStatus(val internetOk: Boolean = false, val restApiOk: Boolean = false)
}

sealed class ElevationState
object NoNetwork : ElevationState()
object Calculating : ElevationState()
data class ElevationData(val points: List<ElePoint> = listOf(), val eleMin: Double = 0.0, val eleMax: Double = 0.0) : ElevationState()

/**
 * A point representing the elevation at a given distance from the departure.
 *
 * @param dist distance in meters
 * @param elevation altitude in meters
 */
data class ElePoint(val dist: Double, val elevation: Double)