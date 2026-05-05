package com.example.rta_app.SOLID.activitys.tracker;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.rta_app.SOLID.api.WorkerHourRepository;
import com.example.rta_app.SOLID.entities.WorkerHous;
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

    public static void update(Context context, WorkerHous workerHous) {
        if (shouldTrack(workerHous)) {
            start(context);
        } else {
            stop(context);
        }
    }

    public static void sync(Context context) {
        Context appContext = context.getApplicationContext();
        WorkerHourRepository workerHourRepository = new WorkerHourRepository(appContext);

        workerHourRepository.getWorkerHous()
                .addOnSuccessListener(workerHous -> update(appContext, workerHous))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "sync(): falha ao ler jornada; rastreamento parado por segurança", e);
                    stop(appContext);
                });
    }

    public static boolean shouldTrack(WorkerHous workerHous) {
        if (workerHous == null) {
            return false;
        }

        boolean hasEntrada = !isBlank(workerHous.getHour_first());
        boolean hasSaidaAlmoco = !isBlank(workerHous.getHour_dinner());
        boolean hasVoltaAlmoco = !isBlank(workerHous.getHour_finish());
        boolean hasFimExpediente = !isBlank(workerHous.getHour_stop());

        return hasEntrada && !hasFimExpediente && (!hasSaidaAlmoco || hasVoltaAlmoco);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static void stop(Context context) {
        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(appContext, TrackingService.class)
                .setAction(TrackingService.ACTION_STOP);
        try {
            appContext.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "stop(): falha ao parar serviço de rastreamento", e);
        }
    }
}
