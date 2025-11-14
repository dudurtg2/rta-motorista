package com.example.rta_app.SOLID.activitys.tracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Data;
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
    private static final String CH_ID = "track_channel";
    private static final int NOTIF_ID = 42;

    private static final long PING_INTERVAL_MS = 15_000L;
    private static final float MIN_DIST_METERS = 0f;

    private FusedLocationProviderClient fused;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIF_ID, notifClara()); // <- mensagem clara
        fused = LocationServices.getFusedLocationProviderClient(this);
        requestUpdates();
    }

    private Notification notifClara() {
        String title = "Rastreamento de jornada";
        String text  = "Sua localização é registrada APENAS durante o expediente. " +
                "No horário de almoço, o rastreamento fica PAUSADO.";

        return new NotificationCompat.Builder(this, CH_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text)) // mostra o texto completo
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }


    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        super.onDestroy();
        if (fused != null) {
            fused.removeLocationUpdates(callback);
        }
    }

    private void createChannel() {
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CH_ID, "Lc", NotificationManager.IMPORTANCE_LOW);
        mgr.createNotificationChannel(ch);
    }

    private Notification notif(String text) {
        return new NotificationCompat.Builder(this, CH_ID)
                .setContentTitle("Lc")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();
    }

    private void requestUpdates() {
        LocationRequest req = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                PING_INTERVAL_MS
        )
                .setMinUpdateDistanceMeters(MIN_DIST_METERS)
                .setWaitForAccurateLocation(false)
                .build();

        fused.requestLocationUpdates(req, callback, Looper.getMainLooper());
    }

    private final LocationCallback callback = new LocationCallback() {
        @Override public void onLocationResult(LocationResult result) {
            Location loc = result.getLastLocation();
            if (loc == null) return;
            enqueuePing(loc);
        }
    };

    private void enqueuePing(Location loc) {
        Data data = new Data.Builder()
                .putDouble("lat", loc.getLatitude())
                .putDouble("lon", loc.getLongitude())
                .putFloat("acc", loc.getAccuracy())
                .putFloat("spd", loc.getSpeed())
                .putFloat("brg", loc.getBearing())
                .putLong("ts", System.currentTimeMillis())
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(PingWorker.class)
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(this).enqueue(req);
    }
}
