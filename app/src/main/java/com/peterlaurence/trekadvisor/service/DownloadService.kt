package com.peterlaurence.trekadvisor.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
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
    private val threadCount = 4
    private val STOP_ACTION = "stop"

    private lateinit var onTapPendingIntent: PendingIntent
    private lateinit var onStopPendingIntent: PendingIntent
    private lateinit var icon: Bitmap

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        @JvmStatic
        @Volatile
        var started = false
    }

    override fun onCreate() {
        EventBus.getDefault().register(this)

        /* Init */
        val notificationIntent = Intent(this, MainActivity::class.java)
        onTapPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val stopIntent = Intent(this, DownloadService::class.java)
        stopIntent.action = STOP_ACTION
        onStopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        notificationManager = NotificationManagerCompat.from(this)

        icon = BitmapFactory.decodeResource(resources,
                R.mipmap.ic_launcher)

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
        /* If the user used the notification action-stop button, stop the service */
        if (intent.action == STOP_ACTION) {
            started = false
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        /* From here, we know that the service is being created by the activity */
        notificationBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.service_download_action))
                .setSmallIcon(R.drawable.ic_file_download_24dp)
                .setLargeIcon(icon)
                .setContentIntent(onTapPendingIntent)
                .addAction(R.drawable.ic_stop_black_24dp, getString(R.string.service_download_stop), onStopPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        startForeground(SERVICE_ID, notificationBuilder.build())

        started = true
        sendStartedStatus()

        requestDownloadSpec()

        return Service.START_NOT_STICKY
    }

    @Subscribe
    fun onRequestDownloadMapEvent(event: RequestDownloadMapEvent) {
        val source = event.source
        var tileSequence = event.tileSequence

        val threadSafeTileIterator = ThreadSafeTileIterator(tileSequence.iterator(), event.numberOfTiles) { p ->
            if (started) {
                handler.post { (this::onDownloadProgress)(p) }
            }
        }

        /* Init the progress bar */
        onDownloadProgress(0.0)

        launchDownloadTask(threadCount, source, threadSafeTileIterator)
    }

    private fun onDownloadProgress(progress: Double) {
        println("on progress $progress")
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
        while (DownloadService.started) {
            val tile = tileIterator.next() ?: break
            bitmapProvider.getBitmap(tile.level, tile.row, tile.col)
            println("downloaded tile ${tile.row}-${tile.col}")
        }
    }
}

private class ThreadSafeTileIterator(private val tileIterator: Iterator<Tile>, val totalSize: Long,
                                     val progressListener: (Double) -> Unit) {
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
        progressListener(progress)
    }
}
