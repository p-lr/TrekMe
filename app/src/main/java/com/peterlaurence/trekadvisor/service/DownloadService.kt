package com.peterlaurence.trekadvisor.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.peterlaurence.trekadvisor.service.event.RequestDownloadMapEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * A [Started service](https://developer.android.com/guide/components/services.html#CreatingAService)
 * to perform Gpx recordings, even if the user is not interacting with the main application.
 *
 * It is started in the foreground to avoid Android 8.0 (API lvl 26)
 * [background execution limit](https://developer.android.com/about/versions/oreo/background.html).
 * So when there is a map download, the user can always see it with the icon on the upper left
 * corner of the device.
 */
class DownloadService : Service() {
    override fun onCreate() {
        EventBus.getDefault().register(this)

        super.onCreate()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    @Subscribe
    public fun onRequestDownloadMapEvent(event: RequestDownloadMapEvent) {
        val url = event.url
        var tileIterable = event.tileIterable

        // Use Glide, Picasso, or a thread pool to download the tiles
        // TODO : handle the case when a download is already pending
    }
}
