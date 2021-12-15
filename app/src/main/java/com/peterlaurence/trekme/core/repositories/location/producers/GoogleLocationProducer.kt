package com.peterlaurence.trekme.core.repositories.location.producers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.peterlaurence.trekme.core.location.InternalGps
import com.peterlaurence.trekme.core.location.Location
import com.peterlaurence.trekme.core.location.LocationProducer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * A [LocationProducer] which uses Google's fused location provider. It combines all possible sources
 * of location data.
 *
 * @author P.Laurence on 26/11/20
 */
class GoogleLocationProducer(private val applicationContext: Context) : LocationProducer {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)
    private val locationRequest = LocationRequest.create().apply {
        interval = 2000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        maxWaitTime = 5000  // 5s
    }
    private val looper = Looper.getMainLooper()

    override val locationFlow: Flow<Location> by lazy {
        makeFlow(applicationContext)
    }

    private fun makeFlow(context: Context): Flow<Location> {
        val permission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return callbackFlow {
            val callback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (loc in locationResult.locations) {
                        val speed = if (loc.speed != 0f) loc.speed else null
                        val altitude = if (loc.altitude != 0.0) loc.altitude else null
                        trySend(Location(loc.latitude, loc.longitude, speed, altitude, loc.time, InternalGps))
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