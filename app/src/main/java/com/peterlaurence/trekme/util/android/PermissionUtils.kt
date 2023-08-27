package com.peterlaurence.trekme.util.android

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

/* Permission-group codes */
const val REQUEST_LOCATION = 1
const val REQUEST_NOTIFICATION = 4
const val REQUEST_NEARBY_WIFI = 5
val MIN_PERMISSIONS_ANDROID_9_AND_BELOW = arrayOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

@RequiresApi(Build.VERSION_CODES.Q)
val PERMISSION_BACKGROUND_LOC = arrayOf(
    Manifest.permission.ACCESS_BACKGROUND_LOCATION
)

/**
 * Android 10 and up only: request background location permission.
 */
fun requestBackgroundLocationPermission(activity: Activity) {
    if (Build.VERSION.SDK_INT < 29) return
    val permissionLocation = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            activity,
            PERMISSION_BACKGROUND_LOC,
            REQUEST_LOCATION
        )
    }
}

fun requestNotificationPermission(activity: Activity) {
    if (Build.VERSION.SDK_INT < 33) return
    val permission = ActivityCompat.checkSelfPermission(activity,
        Manifest.permission.POST_NOTIFICATIONS
    )
    if (permission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION)
    }
}

fun requestNearbyWifiPermission(activity: Activity) {
    if (Build.VERSION.SDK_INT < 33) return
    val permission = ActivityCompat.checkSelfPermission(activity,
        Manifest.permission.NEARBY_WIFI_DEVICES
    )
    if (permission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES), REQUEST_NEARBY_WIFI)
    }
}

fun hasPermissions(activity: Activity, vararg permissions: String): Boolean {
    return permissions.all {
        ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }
}

fun Activity.hasLocationPermission(): Boolean {
    return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}