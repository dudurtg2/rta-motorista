package com.example.rta_app.SOLID.activitys.tracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.concurrent.TimeUnit;

public class TrackingService extends Service {
    private static final String TAG = "TrackingService";

    public static final String ACTION_START = "com.example.rta_app.action.TRACKING_START";
    public static final String ACTION_STOP = "com.example.rta_app.action.TRACKING_STOP";

    private static final String CHANNEL_ID = "track_channel";
    private static final String CHANNEL_NAME = "Rastreamento";
    private static final int NOTIFICATION_ID = 42;
    private static final String UNIQUE_PING_WORK = "tracking_ping_work";

    private static final long PING_INTERVAL_MS = 30_000L;
    private static final float MIN_DIST_METERS = 15f;
    private static final float MAX_ACCEPTABLE_ACCURACY_METERS = 100f;
    private static final long MIN_ENQUEUE_GAP_MS = 20_000L;

    private FusedLocationProviderClient fusedLocationClient;
    private long lastEnqueueAtMs = 0L;
    private Location lastEnqueuedLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ACTION_START;
        Log.d(TAG, "onStartCommand(): action=" + action);

        if (ACTION_STOP.equals(action)) {
            stopTracking();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        requestUpdates();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopTracking();
        super.onDestroy();
    }

    private void stopTracking() {
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private Notification buildNotification() {
        String title = "Rastreamento de jornada";
        String text = "A localização é enviada somente durante o expediente e sem sobrecarregar o aparelho.";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
        );
        manager.createNotificationChannel(channel);
    }

    private void requestUpdates() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, PING_INTERVAL_MS)
                .setMinUpdateDistanceMeters(MIN_DIST_METERS)
                .setWaitForAccurateLocation(false)
                .build();

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult result) {
            if (result == null) {
                return;
            }

            Location location = result.getLastLocation();
            if (location == null) {
                return;
            }

            if (location.hasAccuracy() && location.getAccuracy() > MAX_ACCEPTABLE_ACCURACY_METERS) {
                Log.d(TAG, "Ignorando localização com baixa precisão: " + location.getAccuracy());
                return;
            }

            if (shouldSkipEnqueue(location)) {
                return;
            }

            enqueuePing(location);
            lastEnqueuedLocation = new Location(location);
            lastEnqueueAtMs = System.currentTimeMillis();
        }
    };

    private boolean shouldSkipEnqueue(Location location) {
        long now = System.currentTimeMillis();
        if (lastEnqueuedLocation == null) {
            return false;
        }

        boolean tooSoon = now - lastEnqueueAtMs < MIN_ENQUEUE_GAP_MS;
        boolean barelyMoved = location.distanceTo(lastEnqueuedLocation) < MIN_DIST_METERS;
        if (tooSoon && barelyMoved) {
            Log.d(TAG, "Ping ignorado por throttle local");
            return true;
        }
        return false;
    }

    private void enqueuePing(Location location) {
        Data data = new Data.Builder()
                .putDouble("lat", location.getLatitude())
                .putDouble("lon", location.getLongitude())
                .putFloat("acc", location.getAccuracy())
                .putFloat("spd", location.getSpeed())
                .putFloat("brg", location.getBearing())
                .putLong("ts", System.currentTimeMillis())
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PingWorker.class)
                .setInputData(data)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                UNIQUE_PING_WORK,
                ExistingWorkPolicy.REPLACE,
                request
        );
    }
}
