package com.example.rta_app.SOLID.activitys.tracker;

import android.content.Context;
import android.content.Intent;

import com.example.rta_app.SOLID.services.TokenStorage;


public class LocationTracker {
    // BASE_URL pode continuar fixo aqui (ou você pode guardar no TokenStorage também)
    public static final String BASE_URL = "https://android.lc-transportes.com";

    public static void start(Context context) {
        TokenStorage ts = new TokenStorage(context);
        if (ts.getApiKey().isEmpty()) {
            // ainda não temos token — não inicia.
            return;
        }
        Intent i = new Intent(context, TrackingService.class);
        context.startForegroundService(i);
    }
}
