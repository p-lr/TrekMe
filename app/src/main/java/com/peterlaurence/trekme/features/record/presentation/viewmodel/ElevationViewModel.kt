package com.peterlaurence.trekme.features.record.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.lib.gpx.model.GpxElevationSource
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.core.repositories.recording.*
import com.peterlaurence.trekme.core.lib.gpx.model.GpxElevationSourceInfo
import com.peterlaurence.trekme.core.lib.gpx.model.TrackSegment
import com.peterlaurence.trekme.core.lib.gpx.writeGpx
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource
import com.peterlaurence.trekme.features.record.domain.interactors.RecordingInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject

/**
 * This view-model listens to the [ElevationRepository]'s state changes. Whenever the repository
 * notifies that a gpx file needs to be updated (through [ElevationData.needsUpdate]), this
 * view-model performs the update. When it happens, it means that the repository successfully
 * fetched elevation data from a trusted source.
 * By default, when a gpx file is created by TrekMe, the gpx file metadata indicates that the
 * elevation source is the GPS. When the repository requires an update, the elevation source is
 * necessarily different.
 *
 * This view-model also notifies the UI of other events coming from the [ElevationRepository], such
 * as when an error occurred, or when there's no network.
 *
 * @since 2020/12/13
 **/
@HiltViewModel
class ElevationViewModel @Inject constructor(
    private val recordingInteractor: RecordingInteractor,
    private val repository: ElevationRepository,
    private val gpxRepository: GpxRepository,
    private val appEventBus: AppEventBus,
    private val app: Application,
) : ViewModel() {
    val elevationPoints = repository.elevationRepoState

    init {
        viewModelScope.launch {
            repository.elevationRepoState.collect { state ->
                when (state) {
                    is ElevationData -> {
                        val gpxForElevation = gpxRepository.gpxForElevation.replayCache.firstOrNull()
                        gpxForElevation?.also { gpxForEle ->
                            if (gpxForEle.id == state.id && state.needsUpdate) {
                                updateGpxFileWithTrustedElevations(gpxForEle, state)
                            }
                        }
                    }
                    else -> {
                    } // Nothing to do
                }
            }
        }

        viewModelScope.launch {
            repository.events.collect {
                val ctx = app.applicationContext
                when (it) {
                    ElevationCorrectionErrorEvent -> {
                        val msg = ctx.getString(R.string.elevation_correction_error)
                        appEventBus.postMessage(StandardMessage(msg, showLong = true))
                    }
                    is NoNetworkEvent -> {
                        val msg = if (!it.internetOk) {
                            ctx.getString(R.string.network_required)
                        } else {
                            ctx.getString(R.string.elevation_service_down)
                        }
                        appEventBus.postMessage(WarningMessage(ctx.getString(R.string.warning_title), msg))
                    }
                }
            }
        }
    }

    private fun updateGpxFileWithTrustedElevations(gpxForElevation: GpxForElevation, eleData: ElevationData) {
        val segmentElePoints = eleData.segmentElePoints

        /* Safeguard - don't erase file content with empty data */
        if (segmentElePoints.isEmpty()) return

        val gpx = gpxForElevation.gpx
        /* Update the first track only */
        val newTracks = gpx.tracks.mapIndexed { iTrack, track ->
            if (iTrack == 0) {
                val trackSegments = track.trackSegments.zip(segmentElePoints).map { (segment, segmentElePt) ->
                    if (segment.trackPoints.size == segmentElePt.points.size) {
                        val newTrackPoints = segment.trackPoints.zip(segmentElePt.points).map {
                            it.first.copy(elevation = it.second.elevation)
                        }
                        TrackSegment(newTrackPoints)
                    } else segment
                }
                track.copy(trackSegments = trackSegments)
            } else track
        }
        val newMetadata = gpx.metadata?.copy(
            elevationSourceInfo = GpxElevationSourceInfo(eleData.elevationSource.toGpxElevationSource(), eleData.sampling)
        )
        val newGpx = gpx.copy(tracks = newTracks, metadata = newMetadata)

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                writeGpx(newGpx, FileOutputStream(gpxForElevation.gpxFile))
            }
        }
    }

    fun onUpdateGraph(id: UUID) = viewModelScope.launch {
        val geoRecord = recordingInteractor.getRecord(id)
        if (geoRecord != null) {
            repository.update(geoRecord)
        } else {
            repository.reset()
        }
    }

    private fun ElevationSource.toGpxElevationSource() : GpxElevationSource {
        return when(this) {
            ElevationSource.GPS -> GpxElevationSource.GPS
            ElevationSource.IGN_RGE_ALTI -> GpxElevationSource.IGN_RGE_ALTI
            ElevationSource.UNKNOWN -> GpxElevationSource.UNKNOWN
        }
    }
}
