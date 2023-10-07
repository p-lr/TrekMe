package com.peterlaurence.trekme.core.location.app.producer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.peterlaurence.trekme.core.location.domain.model.InternalGps
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationProducer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * A [LocationProducer] which uses Google's fused location provider. It combines all possible sources
 * of location data.
 *
 * @since 2020/11/26
 */
class GoogleLocationProducer(private val applicationContext: Context) : LocationProducer {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(applicationContext)
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).apply {
        setMaxUpdateDelayMillis(5000)
    }.build()
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
                val timeSource = TimeSource.Monotonic
                var lastSend: TimeSource.Monotonic.ValueTimeMark? = null

                override fun onLocationResult(locationResult: LocationResult) {
                    for (loc in locationResult.locations) {
                        val speed = if (loc.speed != 0f) loc.speed else null
                        val altitude = if (loc.altitude != 0.0) loc.altitude else null
                        val now= timeSource.markNow()
                        val last = lastSend
                        /* Since after the flush, some locations are only a few milliseconds distant,
                         * we need to filter-out those locations. */
                        if (last == null || (now - last > 1.9.seconds)) {
                            lastSend = now
                            trySend(
                                Location(loc.latitude, loc.longitude, speed, altitude, loc.time, now, InternalGps)
                            )
                        }
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