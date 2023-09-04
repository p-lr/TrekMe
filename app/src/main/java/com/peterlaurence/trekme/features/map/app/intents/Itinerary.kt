package com.peterlaurence.trekme.features.map.app.intents

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * First, try to directly use Google maps. Fallback to a generic intent.
 */
fun itineraryToMarker(lat: Double, lon: Double, context: Context): Boolean {
    return runCatching {
        val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lon")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        context.startActivity(mapIntent)
    }.recoverCatching {
        val uri = Uri.parse("geo:q=$lat,$lon")
        val genericIntent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(genericIntent)
    }.isSuccess
}
