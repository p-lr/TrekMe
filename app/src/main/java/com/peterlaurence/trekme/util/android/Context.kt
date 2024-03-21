package com.peterlaurence.trekme.util.android

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

/**
 * Get the activity from the [Context], or throws.
 */
val Context.activity: ComponentActivity
    get() {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is ComponentActivity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        throw IllegalStateException(
            "Expected an activity context but instead found: $ctx"
        )
    }

/**
 * Get the activity from the [Context], or returns null.
 */
tailrec fun Context.getActivityOrNull(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivityOrNull()
    else -> null
}