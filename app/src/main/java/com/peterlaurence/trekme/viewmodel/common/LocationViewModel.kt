package com.peterlaurence.trekme.viewmodel.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * This view model is used every time the location is needed inside a fragment.
 */
class LocationViewModel: ViewModel() {
    private val locationLiveData = MutableLiveData<Location>()

    private var locationProvider: LocationProvider? = null

    fun setLocationProvider(locationProvider: LocationProvider) {
        this.locationProvider = locationProvider
    }

    fun startLocationUpdates() {
        /* Create a local variable to avoid leaking this entire class */
        val liveData = locationLiveData
        locationProvider?.start {
            liveData.postValue(it)
        }
    }

    fun stopLocationUpdates() {
        locationProvider?.stop()
    }

    fun getLocationLiveData(): LiveData<Location> = locationLiveData

    override fun onCleared() {
        locationProvider = null
    }
}