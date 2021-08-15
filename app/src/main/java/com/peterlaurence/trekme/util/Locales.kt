package com.peterlaurence.trekme.util

import android.content.Context
import java.util.*


fun getCurrentLocale(context: Context): Locale {
    return context.resources.configuration.locales.get(0)
}

fun isFrench(context: Context): Boolean {
    return getCurrentLocale(context).language.startsWith("fr")
}

fun isEnglish(context: Context): Boolean {
    return getCurrentLocale(context).language.startsWith("en")
}