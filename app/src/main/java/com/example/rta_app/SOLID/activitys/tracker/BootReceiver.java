package com.example.rta_app.SOLID.activitys.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.rta_app.SOLID.services.TokenStorage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }

        final PendingResult pendingResult = goAsync();
        Context appContext = ctx.getApplicationContext();

        EXECUTOR.execute(() -> {
            try {
                TokenStorage tokenStorage = new TokenStorage(appContext);
                if (!tokenStorage.hasApiKey()) {
                    Log.i(TAG, "Sem API key; rastreamento não será iniciado no boot");
                    return;
                }

                Log.i(TAG, "Boot detectado; sincronizando rastreamento com a jornada salva");
                LocationTracker.sync(appContext);
            } catch (Exception e) {
                Log.e(TAG, "Falha ao processar boot receiver", e);
            } finally {
                pendingResult.finish();
            }
        });
    }
}
