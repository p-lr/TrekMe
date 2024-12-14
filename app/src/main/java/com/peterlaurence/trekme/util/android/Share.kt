package com.peterlaurence.trekme.util.android

import android.content.Context
import android.net.Uri
import androidx.core.app.ShareCompat

fun sendShareIntent(context: Context, uris: List<Uri>) {
    val intentBuilder = ShareCompat.IntentBuilder(context)
        .setType("text/plain")
    uris.forEach { uri ->
        try {
            intentBuilder.addStream(uri)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
    intentBuilder.startChooser()
}