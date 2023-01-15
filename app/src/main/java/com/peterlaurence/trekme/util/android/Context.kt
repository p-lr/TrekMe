package com.peterlaurence.trekme.util.android

import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatActivity

val Context.activity: AppCompatActivity
    get() {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is AppCompatActivity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        throw IllegalStateException(
            "Expected an activity context but instead found: $ctx"
        )
    }

