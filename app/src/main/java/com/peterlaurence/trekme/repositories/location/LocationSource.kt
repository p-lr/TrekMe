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
import com.peterlaurence.trekme.core.model.Location
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

interface LocationSource {
    val locationFlow: SharedFlow<Location>
}

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
                        val speed = if (loc.speed != 0f) loc.speed else null
                        val altitude = if (loc.altitude != 0.0) loc.altitude else null
                        trySend(Location(loc.latitude, loc.longitude, speed, altitude, loc.time))
                    }
                }
            }

            /* Request location updates, with a retry in case of failure */
            fun requestLocationUpdates(): Result<Unit> = runCatching {
                if (permission) {
                    fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            callback,
                            looper
                    ).addOnFailureListener {
                        /* In case of error, re-subscribe after a delay */
                        runBlocking { delay(4000) }
                        if (isActive) {
                            runCatching {
                                fusedLocationClient.removeLocationUpdates(callback)
                            }
                            requestLocationUpdates()
                        }
                    }
                }
            }

            requestLocationUpdates()

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