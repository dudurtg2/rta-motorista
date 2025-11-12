package com.example.rta_app.SOLID.activitys.tracker;

import android.content.Context;
import android.content.Intent;

import com.example.rta_app.SOLID.services.TokenStorage;


public class LocationTracker {
    public static final String BASE_URL = "https://android.lc-transportes.com";

    public static void start(Context context) {
        TokenStorage ts = new TokenStorage(context);
        if (ts.getApiKey().isEmpty()) {
            return;
        }
        Intent i = new Intent(context, TrackingService.class);
        context.startForegroundService(i);
    }
}
