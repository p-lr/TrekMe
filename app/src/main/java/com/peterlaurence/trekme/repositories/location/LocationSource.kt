package com.peterlaurence.trekme.repositories.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

interface LocationSource {
    val locationFlow: SharedFlow<Location>
}

/**
 * [latitude] and [longitude] are in decimal degrees.
 * [altitude] is in meters.
 * [speed] is in meters per second.
 * [time] is the UTC time in milliseconds since January 1, 1970
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
    private val locationRequest = LocationRequest.create().apply {
        interval = 2000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        maxWaitTime = 5000  // 5s
    }
    private val looper = Looper.getMainLooper()

    /**
     * A [SharedFlow] of [Location]s, with a replay of 1.
     * Automatically un-registers underlying callback when there are no collectors.
     * N.B: This shared flow used to be conflated, using a trick reported in
     * https://github.com/Kotlin/kotlinx.coroutines/issues/2408 is fixed
     */
    override val locationFlow: SharedFlow<Location> by lazy {
        makeFlow(applicationContext).shareIn(
                ProcessLifecycleOwner.get().lifecycleScope,
                SharingStarted.WhileSubscribed(),
                1
        )
    }

    private fun makeFlow(context: Context): Flow<Location> {
        val permission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return callbackFlow {
            val callback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    for (loc in locationResult?.locations ?: listOf()) {
                        trySend(Location(loc.latitude, loc.longitude, loc.speed, loc.altitude, loc.time))
                    }
                }
            }

            if (permission) {
                fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        callback,
                        looper
                ).addOnFailureListener {
                    /* In case of error, close the flow, so that the next subscription will trigger
                     * a new flow creation. */
                    close(it)
                }
            }

            launch {
                while (true) {
                    delay(2000)
                    fusedLocationClient.flushLocations()
                }
            }

            awaitClose {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }
    }
}