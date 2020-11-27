package com.peterlaurence.trekme.repositories.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

interface LocationSource {
    // TODO: change to SharedFlow when https://github.com/Kotlin/kotlinx.coroutines/issues/2408 is fixed
    val locationFlow: Flow<Location>
}

/**
 * [latitude] and [longitude] are in decimal degrees.
 * [speed] is in meters per second
 */
data class Location(val latitude: Double = 0.0, val longitude: Double = 0.0, val speed: Float = 0f,
                    val altitude: Double = 0.0, val time: Long = 0)

/**
 * A [LocationSource] which uses Google's fused location provider. It combines all possible sources
 * of location data.
 *
 * @author P.Laurence on 26/11/20
 */
class GoogleLocationSource(private val applicationContext: Context) : LocationSource {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)
    private val locationRequest = LocationRequest().apply {
        interval = 2000
        fastestInterval = 2000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /**
     * A conflated [SharedFlow] of [Location]s, with a replay of 1.
     * Automatically un-registers underlying callback when there are no collectors.
     * TODO: Revert conflate and shareIn when #2408 is fixed
     */
    override val locationFlow: Flow<Location>
        get() = makeFlow(applicationContext).shareIn(
                ProcessLifecycleOwner.get().lifecycleScope,
                SharingStarted.WhileSubscribed(),
                1
        ).conflate()

    private fun makeFlow(context: Context): Flow<Location> {
        val permission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return callbackFlow {
            val callback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    for (loc in locationResult?.locations ?: listOf()) {
                        offer(Location(loc.latitude, loc.longitude, loc.speed, loc.altitude, loc.time))
                    }
                }
            }

            if (permission) {
                fusedLocationClient.requestLocationUpdates(locationRequest,
                        callback, null)
            }

            awaitClose {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }
    }
}