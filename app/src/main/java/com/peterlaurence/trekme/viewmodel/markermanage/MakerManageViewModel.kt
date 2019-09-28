package com.peterlaurence.trekme.viewmodel.markermanage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.map.Map


class MakerManageViewModel : ViewModel() {
    private val geographicCoords = MutableLiveData<GeographicCoords>()
    private val projectedCoords = MutableLiveData<ProjectedCoords>()

    fun getGeographicLiveData(): LiveData<GeographicCoords> = geographicCoords
    fun getProjectedLiveData(): LiveData<ProjectedCoords> = projectedCoords

    /**
     * Called when the view had one of the geographic coordinates edited, and requires an update of
     * the projected coordinates.
     */
    fun onGeographicValuesChanged(map: Map, lat: Double, lon: Double) {
        map.projection?.doProjection(lat, lon)?.also {
            projectedCoords.postValue(ProjectedCoords(X = it[0], Y = it[1]))
        }
    }

    /**
     * Called when the view had one of the projected coordinates edited, and requires an updates of
     * the geographic coordinates.
     */
    fun onProjectedCoordsChanged(map: Map, X: Double, Y: Double) {
        map.projection?.undoProjection(X, Y)?.also {
            geographicCoords.postValue(GeographicCoords(lon = it[0], lat = it[1]))
        }
    }
}

data class GeographicCoords(val lon: Double, val lat: Double)
data class ProjectedCoords(val X: Double, val Y: Double)