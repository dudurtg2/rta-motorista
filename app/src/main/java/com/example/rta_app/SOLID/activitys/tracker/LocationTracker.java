package com.example.rta_app.SOLID.activitys.tracker;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.rta_app.SOLID.services.TokenStorage;

public class LocationTracker {
    private static final String TAG = "LocationTracker";
    public static final String BASE_URL = "https://android.lc-transportes.com";

    public static void start(Context context) {
        Context appContext = context.getApplicationContext();
        TokenStorage tokenStorage = new TokenStorage(appContext);
        if (!tokenStorage.hasApiKey()) {
            Log.i(TAG, "start(): sem API key; serviço não iniciado");
            return;
        }

        try {
            Intent intent = new Intent(appContext, TrackingService.class)
                    .setAction(TrackingService.ACTION_START);
            ContextCompat.startForegroundService(appContext, intent);
        } catch (Exception e) {
            Log.e(TAG, "start(): falha ao iniciar serviço de rastreamento", e);
        }
    }

    public static void stop(Context context) {
        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(appContext, TrackingService.class)
                .setAction(TrackingService.ACTION_STOP);
        appContext.startService(intent);
    }
}
