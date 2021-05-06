package com.peterlaurence.trekme

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * An application that triggers Hilt's code generation and adds an application-level dependency
 * container.
 */
@HiltAndroidApp
class TrekMeApp : Application() {
}