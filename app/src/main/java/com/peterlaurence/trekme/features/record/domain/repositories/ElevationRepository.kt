package com.peterlaurence.trekme.features.record.domain.repositories

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.geotools.deltaTwoPoints
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.georecord.domain.logic.distanceCalculatorFactory
import com.peterlaurence.trekme.features.common.domain.interactors.georecord.getElevationSource
import com.peterlaurence.trekme.features.common.domain.interactors.georecord.hasTrustedElevations
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource
import com.peterlaurence.trekme.features.record.domain.datasource.ElevationDataSource
import com.peterlaurence.trekme.features.record.domain.datasource.model.ApiStatus
import com.peterlaurence.trekme.features.record.domain.datasource.model.Error
import com.peterlaurence.trekme.features.record.domain.datasource.model.NonTrusted
import com.peterlaurence.trekme.features.record.domain.datasource.model.TrustedElevations
import com.peterlaurence.trekme.features.record.domain.model.*
import com.peterlaurence.trekme.util.chunk
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Generates elevation graph data, as state of the exposed [elevationState]. Possible states are:
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
    private val elevationDataSource: ElevationDataSource
) : ElevationStateOwner {
    private val _elevationRepoState = MutableStateFlow<ElevationState>(Calculating)
    override val elevationState: StateFlow<ElevationState> = _elevationRepoState.asStateFlow()

    private val _events = MutableSharedFlow<ElevationEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val events = _events.asSharedFlow()

    private var lastId: UUID? = null
    private var job: Job? = null
    private val sampling = 20

    private val primaryScope = ProcessLifecycleOwner.get().lifecycleScope

    /**
     * Computes elevation data for the given [GeoRecord] and updates the exposed
     * [elevationState].
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
        val firstRouteGroup =
            geoRecord.routeGroups.firstOrNull() ?: return@withContext TrackElevationsSubSampled(
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

        val eleSource =
            if (needsUpdate) ElevationSource.IGN_RGE_ALTI else geoRecord.getElevationSource()

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
                        val points = when (val eleResult = elevationDataSource.getElevations(
                            pts.map { it.lat }, pts.map { it.lon })
                        ) {
                            Error -> {
                                val apiStatus = checkElevationDataSource()
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
                val elePoints = interpolateSegment(
                    geoRecord.hasTrustedElevations(),
                    segmentEleSubSampled,
                    route,
                    distanceOffset
                )
                elePoints.lastOrNull()?.also {
                    distanceOffset = it.dist
                }
                SegmentElePoints(elePoints)
            }

        val subSampledPoints = segmentElevationList.flatMap { it.points }
        val minEle = subSampledPoints.minByOrNull { it.ele }?.ele ?: 0.0
        val maxEle = subSampledPoints.maxByOrNull { it.ele }?.ele ?: 0.0

        return ElevationData(
            geoRecord,
            segmentElePoints,
            minEle,
            maxEle,
            eleSource,
            needsUpdate,
            sampling
        )
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

    private suspend fun checkElevationDataSource(): ApiStatus {
        return elevationDataSource.checkStatus()
    }

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




