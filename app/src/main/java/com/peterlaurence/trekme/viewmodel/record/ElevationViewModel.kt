package com.peterlaurence.trekme.viewmodel.record

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.repositories.recording.*
import com.peterlaurence.trekme.util.gpx.model.TrackSegment
import com.peterlaurence.trekme.util.gpx.writeGpx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.FileOutputStream

class ElevationViewModel @ViewModelInject constructor(
        private val repository: ElevationRepository,
        private val gpxRepository: GpxRepository
) : ViewModel() {
    val elevationPoints = repository.elevationRepoState

    init {
        viewModelScope.launch {
            repository.elevationRepoState.collect { state ->
                when (state) {
                    is ElevationData -> {
                        val gpxForElevation = gpxRepository.gpxForElevation.replayCache.firstOrNull()
                        gpxForElevation?.also { gpxForEle ->
                            if (gpxForEle.id == state.id) {
                                updateGpxFileWithTrustedElevations(gpxForEle, state.points)
                            }
                        }
                    }
                    else -> {
                    } // Nothing to do
                }
            }
        }
    }

    private fun updateGpxFileWithTrustedElevations(gpxForElevation: GpxForElevation, points: List<ElePoint>) {
        /* Update only the first segment of the first track */
        val newTracks = gpxForElevation.gpx.tracks.mapIndexed { iTrack, track ->
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
        val newGpx = gpxForElevation.gpx.copy(tracks = newTracks)

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
