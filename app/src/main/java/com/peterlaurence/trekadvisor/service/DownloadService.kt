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
import com.peterlaurence.trekadvisor.core.mapsource.MapSource
import com.peterlaurence.trekadvisor.core.mapsource.MapSourceCredentials
import com.peterlaurence.trekadvisor.core.mapsource.wmts.Tile
import com.peterlaurence.trekadvisor.core.providers.generic.GenericBitmapProvider
import com.peterlaurence.trekadvisor.core.providers.generic.GenericBitmapProviderIgn
import com.peterlaurence.trekadvisor.menu.mapcreate.providers.ign.IgnWmtsDialog
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
    private val threadCount = 4

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
            notificationManager.createNotificationChannel(mChannel)
            notificationBuilder.setChannelId(NOTIFICATION_ID)
        }
        val notification = notificationBuilder.build()

        startForeground(SERVICE_ID, notification)

        started = true
        sendStartedStatus()

        requestDownloadSpec()

        return Service.START_NOT_STICKY
    }

    @Subscribe
    fun onRequestDownloadMapEvent(event: RequestDownloadMapEvent) {
        val source = event.source
        var tileSequence = event.tileSequence

        val threadSafeTileIterator = ThreadSafeTileIterator(tileSequence.iterator(), event.numberOfTiles)
        launchDownloadTask(threadCount, source, threadSafeTileIterator)
    }

    private fun sendStartedStatus() {
        EventBus.getDefault().post(DownloadServiceStatus(started))
    }

    private fun requestDownloadSpec() {
        EventBus.getDefault().post(IgnWmtsDialog.DownloadSpecRequest())
    }
}

private fun launchDownloadTask(threadCount: Int, source: MapSource, tileIterator: ThreadSafeTileIterator) {
    for (i in 0 until threadCount) {
        when (source) {
            MapSource.IGN -> {
                val ignCredentials = MapSourceCredentials.getIGNCredentials()!!

                val bitmapProvider = GenericBitmapProviderIgn(ignCredentials)
                val downloadThread = TileDownloadThread(tileIterator, bitmapProvider)
                downloadThread.start()
            }
            else -> {
            }
        }
    }
}


private class TileDownloadThread(private val tileIterator: ThreadSafeTileIterator, private val bitmapProvider: GenericBitmapProvider) : Thread() {
    val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.RGB_565)

    init {
        val options = BitmapFactory.Options()
        options.inBitmap = bitmap
        options.inPreferredConfig = Bitmap.Config.RGB_565
        bitmapProvider.setBitmapOptions(options)
    }

    override fun run() {
        while (true) {
            val tile = tileIterator.next() ?: break
            bitmapProvider.getBitmap(tile.level, tile.row, tile.col)
            println("downloaded tile ${tile.row}-${tile.col}")
        }
    }
}

private class ThreadSafeTileIterator(private val tileIterator: Iterator<Tile>, val totalSize: Long) {
    /* Progress in percent */
    var progress = 0.0
    private var tileIndex: Long = 0

    fun next(): Tile? {
        return synchronized(this) {
            if (tileIterator.hasNext()) {
                updateProgress()
                tileIterator.next()
            } else {
                progress = 100.0
                null
            }
        }
    }

    private fun updateProgress() {
        tileIndex++
        progress = tileIndex * 100.0 / totalSize
        println("progress  : $progress")
    }
}
