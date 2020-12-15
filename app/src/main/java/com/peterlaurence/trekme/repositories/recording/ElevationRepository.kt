@file:Suppress("LocalVariableName")

package com.peterlaurence.trekme.repositories.recording

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.core.geotools.deltaTwoPoints
import com.peterlaurence.trekme.util.gpx.model.Gpx
import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress

class ElevationRepository(
        private val dispatcher: CoroutineDispatcher,
        private val ioDispatcher: CoroutineDispatcher,
        private val gpxRepository: GpxRepository
) {
    private val _elevationPoints = MutableStateFlow<ElevationState>(Calculating)
    val elevationPoints: StateFlow<ElevationState> = _elevationPoints.asStateFlow()

    private var lastGpxId: Int? = null
    private var lastTargetWidth: Int? = null
    private var job: Job? = null

    /**
     * Updates the elevation statistics, if necessary. The [Gpx] must be provided along with a
     * unique [id]. This is necessary to identify whether we should cancel an ongoing work or not.
     *
     * @param targetWidth The actual amount of pixels in horizontal dimension. If the tracks has too
     * many points, it will be sub-sampled.
     */
    fun update(targetWidth: Int) {
        val gpxData: GpxForElevation = gpxRepository.gpxForElevation ?: return
        val (gpx, id) = gpxData
        if (id != lastGpxId || targetWidth != lastTargetWidth) {
            job?.cancel()
            job = ProcessLifecycleOwner.get().lifecycleScope.launch {
                _elevationPoints.emit(Calculating)
                val data = gpxToElevationData(gpx, targetWidth)
                _elevationPoints.emit(data)
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
        var minEle: Double? = null
        var maxEle: Double? = null
        val points = gpx.tracks.firstOrNull()?.trackSegments?.firstOrNull()?.trackPoints?.mapNotNull { pt ->
            val lastPt_ = lastPt
            val lastEle = lastPt_?.elevation
            val newEle = pt.elevation
            if (lastEle != null && newEle != null) {
                dist += deltaTwoPoints(lastPt_.latitude, lastPt_.longitude, lastEle,
                        pt.latitude, pt.longitude, newEle)
            }
            if (newEle != null) {
                if (minEle == null) minEle = newEle
                minEle?.also {
                    if (newEle < it) minEle = newEle
                }
                if (maxEle == null) maxEle = newEle
                maxEle?.also {
                    if (newEle > it) maxEle = newEle
                }
            }
            lastPt = pt
            if (newEle != null) ElePoint(dist, newEle) else null
        }

        val minEle_ = minEle
        val maxEle_ = maxEle
        if (points != null && minEle_ != null && maxEle_ != null) {
            ElevationData(points.subSample(targetWidth), minEle_, maxEle_)
        } else {
            ElevationData(listOf(), 0.0, 0.0)
        }
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