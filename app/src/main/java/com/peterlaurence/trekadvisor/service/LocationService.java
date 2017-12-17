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
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.peterlaurence.trekadvisor.MainActivity;
import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.events.LocationEvent;
import com.peterlaurence.trekadvisor.menu.events.RecordGpxStartEvent;
import com.peterlaurence.trekadvisor.menu.events.RecordGpxStopEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class LocationService extends Service {
    private static final String NOTIFICATION_ID = "peterlaurence.LocationService";
    private static final int SERVICE_ID = 126585;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    public LocationService() {
    }

    @Override
    public void onCreate() {
        EventBus.getDefault().register(this);

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("LocationServiceThread",
                Thread.MIN_PRIORITY);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        mServiceHandler.handleMessage(new Message());

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this.getApplicationContext());
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    EventBus.getDefault().post(new LocationEvent(location));
                }
            }
        };

        startLocationUpdates();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO : remove Toast
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);

        Notification.Builder notificationBuilder = new Notification.Builder(getApplicationContext())
                .setContentTitle(getText(R.string.app_name))
                .setTicker("App Is Running")
                .setContentText(getText(R.string.service_action))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            //This only needs to be run on Devices on Android O and above
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

        return START_NOT_STICKY;
    }

    @Subscribe
    public void onRecordGpxStartEvent(RecordGpxStartEvent event) {
    }

    @Subscribe
    public void onRecordGpxStopEvent(RecordGpxStopEvent event) {
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        // TODO : remove Toast
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();

        stopLocationUpdates();
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
     * The handler on the {@link HandlerThread} of this service.
     */
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            /* Example of task submitting */
            post(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Restore interrupt status.
                    Thread.currentThread().interrupt();
                }
            });

        }
    }
}
