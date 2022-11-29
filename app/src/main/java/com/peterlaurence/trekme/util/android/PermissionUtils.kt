package com.peterlaurence.trekme.util.android

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

/* Permission-group codes */
const val REQUEST_LOCATION = 1
const val REQUEST_MAP_CREATION = 2
const val REQUEST_STORAGE = 3
const val REQUEST_NOTIFICATION = 4
const val REQUEST_NEARBY_WIFI = 5
val PERMISSION_STORAGE = arrayOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)

@RequiresApi(Build.VERSION_CODES.Q)
val PERMISSION_BACKGROUND_LOC = arrayOf(
    Manifest.permission.ACCESS_BACKGROUND_LOCATION
)

val PERMISSION_LOCATION = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
)

val PERMISSIONS_MAP_CREATION = arrayOf(
    Manifest.permission.INTERNET
)

/**
 * Checks whether the app has permission to access fine location and (for Android < 10) to
 * write to device storage.
 * If the app does not have the requested permissions then the user will be prompted.
 */
fun requestMinimalPermissions(activity: Activity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        /* We absolutely need storage perm under Android 10 */
        val permissionWrite = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionWrite != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                PERMISSION_STORAGE,
                REQUEST_STORAGE
            )
        }
    }

    /* Always ask for location perm - even for Android 10 */
    val permissionLocation = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
    if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            activity,
            PERMISSION_LOCATION,
            REQUEST_LOCATION
        )
    }
}

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

fun shouldInit(activity: Activity): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        val permissionWrite = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return permissionWrite == PackageManager.PERMISSION_GRANTED
    }
    return true // we don't need write permission for Android >= 10
}

fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
    if (context != null) {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
    }
    return true
}