package com.peterlaurence.trekme.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.peterlaurence.trekme.MainActivity
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.mapbuilder.buildFromMapSpec
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.MapSourceCredentials
import com.peterlaurence.trekme.core.mapsource.wmts.MapSpec
import com.peterlaurence.trekme.core.mapsource.wmts.Tile
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.providers.bitmap.BitmapProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttp
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttpAuth
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderIgn
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderIgnSpain
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderOSM
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderUSGS
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.model.providers.layers.LayerForSource
import com.peterlaurence.trekme.service.event.DownloadServiceStatusEvent
import com.peterlaurence.trekme.service.event.MapDownloadEvent
import com.peterlaurence.trekme.service.event.RequestDownloadMapEvent
import com.peterlaurence.trekme.service.event.Status
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


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
    private val notificationChannelId = "peterlaurence.DownloadService"
    private val downloadServiceNofificationId = 128565
    private val threadCount = 8
    private val stopAction = "stop"

    private lateinit var onTapPendingIntent: PendingIntent
    private lateinit var onStopPendingIntent: PendingIntent
    private lateinit var icon: Bitmap

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var destDir: File

    private val progressEvent = MapDownloadEvent(Status.PENDING, 0.0)

    companion object {
        @JvmStatic
        @Volatile
        var started = false
    }

    override fun onCreate() {
        /* Init */
        val notificationIntent = Intent(this, MainActivity::class.java)
        onTapPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val stopIntent = Intent(this, DownloadService::class.java)
        stopIntent.action = stopAction
        onStopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        notificationManager = NotificationManagerCompat.from(this)

        icon = BitmapFactory.decodeResource(resources,
                R.mipmap.ic_launcher)

        super.onCreate()
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
        if (intent.action == stopAction) {
            started = false
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        /* From here, we know that the service is being created by the activity */
        notificationBuilder = NotificationCompat.Builder(applicationContext, notificationChannelId)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.service_download_action))
                .setSmallIcon(R.drawable.ic_file_download_24dp)
                .setLargeIcon(icon)
                .setContentIntent(onTapPendingIntent)
                .setColor(getColor(R.color.colorAccent))
                .addAction(R.drawable.ic_stop_black_24dp, getString(R.string.service_download_stop), onStopPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOnlyAlertOnce(true)

        /* This is only needed on Devices on Android O and above */
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val chan = NotificationChannel(notificationChannelId, getText(R.string.service_download_name), NotificationManager.IMPORTANCE_DEFAULT)
            chan.enableLights(true)
            chan.lightColor = Color.MAGENTA
            notificationManager.createNotificationChannel(chan)
            notificationBuilder.setChannelId(notificationChannelId)
        }

        startForeground(downloadServiceNofificationId, notificationBuilder.build())

        started = true
        sendStartedStatus()

        /* Get ready for download and request download spec */
        progressEvent.progress = 0.0

        handler.post {
            processRequestDownloadMapEvent()
        }

        return START_NOT_STICKY
    }

    private fun processRequestDownloadMapEvent() {
        val event = EventBus.getDefault().getStickyEvent(RequestDownloadMapEvent::class.java)
                ?: return
        val source = event.source
        val tileSequence = event.mapSpec.tileSequence

        val threadSafeTileIterator = ThreadSafeTileIterator(tileSequence.iterator(), event.numberOfTiles) { p ->
            if (started) {
                handler.post { (this::onDownloadProgress)(p) }

                /* Post-process if download reaches 100% */
                if (p == 100.0) {
                    postProcess(event.mapSpec)
                }
            }
        }

        /* Init the progress bar */
        onDownloadProgress(0.0)

        /* Create the destination folder, or else fail-fast */
        val destDirRes = createDestDir()
        if (destDirRes != null) {
            destDir = destDirRes
        } else {
            /* Storage issue, warn and stop the service */
            notifyDownloadFinished(getText(R.string.service_download_bad_storage))
            EventBus.getDefault().post(MapDownloadEvent(Status.STORAGE_ERROR))
            stopSelf()
            return
        }

        /* A writer which has a folder for each level, and a folder for each row. It does that with
         * using indexes instead of real level, row and col numbers. This greatly simplifies how a
         * tile is later retrieved from a bitmap provider. */
        val tileWriter = object : TileWriter(destDir) {
            override fun write(tile: Tile, bitmap: Bitmap) {
                val tileDir = File(destDir, tile.indexLevel.toString() + File.separator + tile.indexRow.toString())
                tileDir.mkdirs()
                val tileFile = File(tileDir, tile.indexCol.toString() + ".jpg")
                try {
                    val out = FileOutputStream(tileFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.flush()
                    out.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /* Specific to OSM, don't use more than 2 threads */
        val effectiveThreadCount = if (source == MapSource.OPEN_STREET_MAP) 2 else threadCount
        launchDownloadTask(effectiveThreadCount, source, threadSafeTileIterator, tileWriter)
    }

    private fun createDestDir(): File? = runBlocking {
        /* Create a new folder */
        val date = Date()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.ENGLISH)
        val folderName = "map-" + dateFormat.format(date)
        val destFolder = File(Settings.getDownloadDir(), folderName)

        if (destFolder.mkdirs()) {
            destFolder
        } else {
            null
        }
    }

    private fun onDownloadProgress(progress: Double) {
        /* Update the notification */
        notificationBuilder.setProgress(100, progress.toInt(), false)
        notificationBuilder.setWhen(0)
        notificationBuilder.setOngoing(false)
        try {
            notificationManager.notify(downloadServiceNofificationId, notificationBuilder.build())
        } catch (e: RuntimeException) {
            // can't figure out why it's (rarely) thrown. Ignore it for now
        }


        /* Send a message carrying the progress info */
        progressEvent.progress = progress
        EventBus.getDefault().post(progressEvent)
    }

    private fun postProcess(mapSpec: MapSpec) {
        val calibrationPoints = mapSpec.calibrationPoints

        /* Calibrate */
        fun calibrate(map: Map) {
            map.projection = MercatorProjection()
            map.mapGson.calibration.calibration_method = MapLoader.CALIBRATION_METHOD.SIMPLE_2_POINTS.name
            map.mapGson.calibration.calibration_points = calibrationPoints.toList()
            map.calibrate()
            MapLoader.addMap(map)
        }

        val map = buildFromMapSpec(mapSpec, destDir, ".jpg")

        handler.post {
            calibrate(map)

            /* Notify that the download is finished correctly*/
            notifyDownloadFinished(getText(R.string.service_download_finished))

            /* Finally, stop the service */
            stopSelf()
        }
    }

    private fun notifyDownloadFinished(message: CharSequence) {
        /* Update the notification */
        notificationBuilder.setContentText(message)
        notificationBuilder.setProgress(0, 0, false)
        notificationBuilder.mActions.clear()
        notificationManager.notify(downloadServiceNofificationId, notificationBuilder.build())

        /* Tell the rest of the app that the download is finished */
        EventBus.getDefault().post(MapDownloadEvent(Status.FINISHED))
    }

    private fun sendStartedStatus() {
        EventBus.getDefault().post(DownloadServiceStatusEvent(started))
    }
}

private fun launchDownloadTask(threadCount: Int, source: MapSource, tileIterator: ThreadSafeTileIterator,
                               tileWriter: TileWriter) {
    for (i in 0 until threadCount) {
        when (source) {
            MapSource.IGN -> {
                val ignCredentials = MapSourceCredentials.getIGNCredentials()!!

                val layerRealName = LayerForSource.resolveLayerName(source)
                val urlTileBuilder = UrlTileBuilderIgn(ignCredentials.api ?: "", layerRealName)
                val tileStreamProvider = TileStreamProviderHttpAuth(urlTileBuilder, ignCredentials.user
                        ?: "",
                        ignCredentials.pwd ?: "")
                val bitmapProvider = BitmapProvider(tileStreamProvider)
                val downloadThread = TileDownloadThread(tileIterator, bitmapProvider, tileWriter)
                downloadThread.start()
            }
            MapSource.USGS -> {
                val urlTileBuilder = UrlTileBuilderUSGS()
                val tileStreamProvider = TileStreamProviderHttp(urlTileBuilder)
                val bitmapProvider = BitmapProvider(tileStreamProvider)
                val downloadThread = TileDownloadThread(tileIterator, bitmapProvider, tileWriter)
                downloadThread.start()
            }
            MapSource.OPEN_STREET_MAP -> {
                val urlTileBuilder = UrlTileBuilderOSM()
                val tileStreamProvider = TileStreamProviderHttp(urlTileBuilder)
                val bitmapProvider = BitmapProvider(tileStreamProvider)
                val downloadThread = TileDownloadThread(tileIterator, bitmapProvider, tileWriter)
                downloadThread.start()
            }
            MapSource.IGN_SPAIN -> {
                val urlTileBuilder = UrlTileBuilderIgnSpain()
                val tileStreamProvider = TileStreamProviderHttp(urlTileBuilder)
                val bitmapProvider = BitmapProvider(tileStreamProvider)
                val downloadThread = TileDownloadThread(tileIterator, bitmapProvider, tileWriter)
                downloadThread.start()
            }
        }
    }
}


private class TileDownloadThread(private val tileIterator: ThreadSafeTileIterator,
                                 private val bitmapProvider: BitmapProvider,
                                 private val tileWriter: TileWriter) : Thread() {
    val bitmap: Bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.RGB_565)

    init {
        val options = BitmapFactory.Options()
        options.inBitmap = bitmap
        options.inPreferredConfig = Bitmap.Config.RGB_565
        bitmapProvider.setBitmapOptions(options)
    }

    override fun run() {
        while (DownloadService.started) {
            val tile = tileIterator.next() ?: break
            bitmapProvider.getBitmap(row = tile.row, col = tile.col, zoomLvl = tile.level).also {
                /* Only write if there was no error */
                if (it != null && DownloadService.started) {
                    tileWriter.write(tile, bitmap)
                }
            }
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

private abstract class TileWriter(val destDir: File) {
    abstract fun write(tile: Tile, bitmap: Bitmap)
}
