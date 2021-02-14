package com.peterlaurence.trekme.viewmodel.record

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.events.StandardMessage
import com.peterlaurence.trekme.core.events.WarningMessage
import com.peterlaurence.trekme.repositories.recording.*
import com.peterlaurence.trekme.util.gpx.model.ElevationSourceInfo
import com.peterlaurence.trekme.util.gpx.model.TrackSegment
import com.peterlaurence.trekme.util.gpx.writeGpx
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ElevationViewModel @Inject constructor(
        private val repository: ElevationRepository,
        private val gpxRepository: GpxRepository,
        private val appEventBus: AppEventBus,
        private val app: Application
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
        val points = eleData.points
        val gpx = gpxForElevation.gpx
        /* Update only the first segment of the first track */
        val newTracks = gpx.tracks.mapIndexed { iTrack, track ->
            if (iTrack == 0) {
                val trackSegments = track.trackSegments.mapIndexed { iSeg, trackSegment ->
                    if (iSeg == 0) {
                        if (trackSegment.trackPoints.size == points.size) {
                            val newTrackPoints = trackSegment.trackPoints.zip(points).map {
                                it.first.copy(elevation = it.second.elevation)
                            }
                            TrackSegment(newTrackPoints)
                        } else trackSegment
                    } else trackSegment
                }
                track.copy(trackSegments = trackSegments)
            } else track
        }
        val newMetadata = gpx.metadata?.copy(elevationSourceInfo = ElevationSourceInfo(eleData.elevationSource, eleData.sampling))
        val newGpx = gpx.copy(tracks = newTracks, metadata = newMetadata)

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                writeGpx(newGpx, FileOutputStream(gpxForElevation.gpxFile))
            }
        }
    }

    fun onUpdateGraph() = viewModelScope.launch {
        gpxRepository.gpxForElevation.collect {
            repository.update(it)
        }
    }
}
