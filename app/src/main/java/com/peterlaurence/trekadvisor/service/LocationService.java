package com.peterlaurence.trekadvisor.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.peterlaurence.trekadvisor.MainActivity;
import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.TrekAdvisorContext;
import com.peterlaurence.trekadvisor.menu.events.RecordGpxStartEvent;
import com.peterlaurence.trekadvisor.menu.events.RecordGpxStopEvent;
import com.peterlaurence.trekadvisor.service.event.GpxFileWriteEvent;
import com.peterlaurence.trekadvisor.service.event.LocationServiceStatus;
import com.peterlaurence.trekadvisor.util.gpx.GPXWriter;
import com.peterlaurence.trekadvisor.util.gpx.model.Gpx;
import com.peterlaurence.trekadvisor.util.gpx.model.Track;
import com.peterlaurence.trekadvisor.util.gpx.model.TrackPoint;
import com.peterlaurence.trekadvisor.util.gpx.model.TrackSegment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocationService extends Service {
    private static final String GPX_VERSION = "1.1";
    private static final String NOTIFICATION_ID = "peterlaurence.LocationService";
    private static final int SERVICE_ID = 126585;
    private Looper mServiceLooper;
    private Handler mServiceHandler;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private List<TrackPoint> mTrackPoints;

    private boolean mStarted = false;

    public LocationService() {
    }

    @Override
    public void onCreate() {
        EventBus.getDefault().register(this);

        /* Start up the thread running the service.  Note that we create a separate thread because
         * the service normally runs in the process's main thread, which we don't want to block.
         * We also make it background priority so CPU-intensive work will not disrupt our UI.
         */
        HandlerThread thread = new HandlerThread("LocationServiceThread",
                Thread.MIN_PRIORITY);
        thread.start();

        /* Get the HandlerThread's Looper and use it for our Handler */
        mServiceLooper = thread.getLooper();
        mServiceHandler = new Handler(mServiceLooper);

        mServiceHandler.handleMessage(new Message());

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this.getApplicationContext());
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /* Create the Gpx instance */
        mTrackPoints = new ArrayList<>();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    mServiceHandler.post(() -> {
                        TrackPoint.Builder pointBuilder = new TrackPoint.Builder();
                        pointBuilder.setLatitude(location.getLatitude());
                        pointBuilder.setLongitude(location.getLongitude());
                        pointBuilder.setElevation(location.getAltitude());

                        TrackPoint trackPoint = pointBuilder.build();
                        mTrackPoints.add(trackPoint);
                    });
                }
            }
        };

        startLocationUpdates();
    }

    /**
     * When we stop recording the location events, create a {@link Gpx} object for further
     * serialization. <br>
     * Whatever the outcome of this process, a {@link GpxFileWriteEvent} is emitted in the
     * LocationServiceThread.
     */
    private void createGpx() {
        mServiceHandler.post(() -> {
            ArrayList<TrackSegment> trkSegList = new ArrayList<>();
            trkSegList.add(new TrackSegment(mTrackPoints));

            Track track = new Track(trkSegList, TrekAdvisorContext.APP_FOLDER_NAME + " track");

            ArrayList<Track> trkList = new ArrayList<>();
            trkList.add(track);

            Gpx gpx = new Gpx(trkList, TrekAdvisorContext.APP_FOLDER_NAME, GPX_VERSION);
            try {
                if (!TrekAdvisorContext.DEFAULT_RECORDINGS_DIR.exists()) {
                    TrekAdvisorContext.DEFAULT_RECORDINGS_DIR.mkdir();
                }

                Date date = new Date();
                DateFormat dateFormat = new SimpleDateFormat("dd\\MM\\yyyy-HH:mm:ss", Locale.ENGLISH);
                String gpxFileName = "track-" + dateFormat.format(date) + ".gpx";
                File gpxFile = new File(TrekAdvisorContext.DEFAULT_RECORDINGS_DIR, gpxFileName);
                FileOutputStream fos = new FileOutputStream(gpxFile);
                GPXWriter.INSTANCE.write(gpx, fos);
            } catch (Exception e) {
                // for instance, don't care : we want to stop the service anyway
                // TODO : warn the user that the gpx file could not be saved
            } finally {
                EventBus.getDefault().post(new GpxFileWriteEvent());
            }
        });
    }

    /**
     * Called when the service is started.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);

        Notification.Builder notificationBuilder = new Notification.Builder(getApplicationContext())
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.service_action))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            /* This is only needed on Devices on Android O and above */
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_ID, getText(R.string.service_description), NotificationManager.IMPORTANCE_DEFAULT);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.MAGENTA);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
            }
            notificationBuilder.setChannelId(NOTIFICATION_ID);
        }
        Notification notification = notificationBuilder.build();

        startForeground(SERVICE_ID, notification);

        mStarted = true;
        sendStatus();

        return START_NOT_STICKY;
    }

    @Subscribe
    public void onRecordGpxStartEvent(RecordGpxStartEvent event) {
    }

    /**
     * Create and write a new gpx file. <br>
     * After this is done, a {@link GpxFileWriteEvent} is emitted through event bus so the service
     * can stop properly.
     */
    @Subscribe
    public void onRecordGpxStopEvent(RecordGpxStopEvent event) {
        createGpx();
    }

    /**
     * Self-respond to a {link GpxFileWriteEvent} emitted by the service. <br>
     * When a GPX file has just been written, stop the service and send the status.
     */
    @Subscribe
    public void onGpxFileWriteEvent(GpxFileWriteEvent event) {
        stopSelf();
        mStarted = false;
        sendStatus();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        mServiceLooper.quitSafely();
        EventBus.getDefault().unregister(this);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                mServiceLooper);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * Send the started/stopped boolean status of the service.
     */
    private void sendStatus() {
        EventBus.getDefault().post(new LocationServiceStatus(mStarted));
    }
}
