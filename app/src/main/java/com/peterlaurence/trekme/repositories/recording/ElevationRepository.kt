package com.peterlaurence.trekme.repositories.recording

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.util.gpx.model.Gpx
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ElevationRepository(private val dispatcher: CoroutineDispatcher) {
    private val _elevationPoints = MutableStateFlow<ElevationState>(Calculating)
    val elevationPoints: StateFlow<ElevationState> = _elevationPoints.asStateFlow()

    private var lastGpxId: Int? = null
    private var job: Job? = null

    /**
     * Updates the elevation statistics, if necessary. The [Gpx] must be provided along with a
     * unique [id]. This is necessary to identify whether we should cancel an ongoing work or not.
     */
    fun setGpx(gpx: Gpx, id: Int) {
        if (id != lastGpxId) {
            job?.cancel()
            job = ProcessLifecycleOwner.get().lifecycleScope.launch {
                _elevationPoints.emit(Calculating)
                val data = gpxToElevationData(gpx)
                _elevationPoints.emit(data)
            }

            /* Avoid keeping reference on data */
            job?.invokeOnCompletion {
                job = null
            }
            lastGpxId = id
        }
    }

    private suspend fun gpxToElevationData(gpx: Gpx): ElevationData = withContext(dispatcher) {
        ElevationData(listOf(
                ElePoint(0.0, 55.0),
                ElePoint(150.0, 100.0),
                ElePoint(1000.0, -50.0),
                ElePoint(1500.0, 30.0)
        ), -50.0, 100.0)
    }
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