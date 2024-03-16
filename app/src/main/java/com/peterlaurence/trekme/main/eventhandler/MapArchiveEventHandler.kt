package com.peterlaurence.trekme.main.eventhandler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.interactors.ArchiveMapInteractor
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.maparchive.MapArchiveEvents
import com.peterlaurence.trekme.util.android.activity
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle

/**
 * Handles application wide map archive events. This class is intended to be used by the main
 * activity only.
 */
@Composable
fun MapArchiveEventHandler(appEventBus: AppEventBus, mapArchiveEvents: MapArchiveEvents) {
    /* Used for notifications */
    var builder: Notification.Builder? = null
    var notifyMgr: NotificationManager? = null

    val activity = LocalContext.current.activity

    /**
     * A [Notification] is sent to the user showing the progression in percent. The
     * [NotificationManager] only process one notification at a time, which is handy since
     * it prevents the application from using too much cpu.
     */
    val onZipProgressEvent = { event: ArchiveMapInteractor.ZipProgressEvent ->
        val notificationChannelId = "trekadvisor_map_save"
        if (builder == null || notifyMgr == null) {
            try {
                notifyMgr =
                    activity.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            } catch (e: Exception) {
                // notifyMgr will be null
            }
            if (Build.VERSION.SDK_INT >= 26) {
                //This only needs to be run on Devices on Android O and above
                val channel = NotificationChannel(
                    notificationChannelId,
                    activity.getText(R.string.archive_dialog_title),
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.enableLights(true)
                channel.lightColor = Color.YELLOW
                notifyMgr?.createNotificationChannel(channel)
                builder = Notification.Builder(activity, notificationChannelId)
            } else {
                @Suppress("DEPRECATION")
                builder = Notification.Builder(activity)
            }

            builder?.setSmallIcon(R.drawable.ic_map_black_24dp)
                ?.setContentTitle(activity.getString(R.string.archive_dialog_title))
            notifyMgr?.notify(event.mapId.hashCode(), builder?.build())
        }
        builder?.setContentText(
            String.format(
                activity.getString(R.string.archive_notification_msg),
                event.mapName
            )
        )
        builder?.setProgress(100, event.p, false)
        notifyMgr?.notify(event.mapId.hashCode(), builder!!.build())
    }

    val onZipFinishedEvent = l@{ event: ArchiveMapInteractor.ZipFinishedEvent ->
        val archiveOkMsg = activity.getString(R.string.archive_snackbar_finished)
        val builder = builder ?: return@l
        /* When the loop is finished, updates the notification */
        builder.setContentText(archiveOkMsg) // Removes the progress bar
            .setProgress(0, 0, false)
        notifyMgr?.notify(event.mapId.hashCode(), builder.build())
        appEventBus.postMessage(StandardMessage(archiveOkMsg))
    }

    LaunchedEffectWithLifecycle(mapArchiveEvents.mapArchiveEventFlow) { event ->
        when (event) {
            is ArchiveMapInteractor.ZipProgressEvent -> onZipProgressEvent(event)
            is ArchiveMapInteractor.ZipFinishedEvent -> onZipFinishedEvent(event)
            ArchiveMapInteractor.ZipError -> {
                //TODO: Display a warning
            }

            is ArchiveMapInteractor.ZipCloseEvent -> {
                // Nothing to do
            }
        }
    }
}