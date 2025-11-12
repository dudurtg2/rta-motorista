package com.example.rta_app.SOLID.activitys.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.rta_app.SOLID.services.TokenStorage;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        TokenStorage ts = new TokenStorage(ctx);
        if (ts.getApiKey().isEmpty()) return;

        String a = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(a)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            LocationTracker.start(ctx);
        }
    }
}
