package com.peterlaurence.trekadvisor.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.peterlaurence.trekadvisor.MainActivity
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.service.event.DownloadServiceStatus
import com.peterlaurence.trekadvisor.service.event.RequestDownloadMapEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * A [Started service](https://developer.android.com/guide/components/services.html#CreatingAService)
 * to perform map download, even if the user is not interacting with the main application.
 *
 * It is started in the foreground to avoid Android 8.0 (API lvl 26)
 * [background execution limit](https://developer.android.com/about/versions/oreo/background.html).
 * So when there is a map download, the user can always see it with the icon on the upper left
 * corner of the device.
 */
class DownloadService : Service() {
    private val NOTIFICATION_ID = "peterlaurence.DownloadService"
    private val SERVICE_ID = 128565
    private var started = false

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

    /**
     * Called when the service is started.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val icon = BitmapFactory.decodeResource(resources,
                R.mipmap.ic_launcher)

        val notificationBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.service_download_action))
                .setSmallIcon(R.drawable.ic_file_download_24dp)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            /* This is only needed on Devices on Android O and above */
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel(NOTIFICATION_ID, getText(R.string.service_download_name), NotificationManager.IMPORTANCE_DEFAULT)
            mChannel.enableLights(true)
            mChannel.lightColor = Color.MAGENTA
            notificationManager?.createNotificationChannel(mChannel)
            notificationBuilder.setChannelId(NOTIFICATION_ID)
        }
        val notification = notificationBuilder.build()

        startForeground(SERVICE_ID, notification)

        started = true
        sendStartedStatus()

        return Service.START_NOT_STICKY
    }

    @Subscribe
    public fun onRequestDownloadMapEvent(event: RequestDownloadMapEvent) {
        val source = event.source
        var tileSequence = event.tileSequence

        println("Download service : request download for $source")

        // Use Glide, Picasso, or a thread pool to download the tiles
        // TODO : handle the case when a download is already pending
    }

    private fun sendStartedStatus() {
        EventBus.getDefault().post(DownloadServiceStatus(started))
    }
}
