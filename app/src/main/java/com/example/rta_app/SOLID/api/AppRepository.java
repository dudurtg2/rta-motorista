package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.entities.App;
import com.example.rta_app.SOLID.services.ApiClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Request;

public class AppRepository {

    private static final String TAG = "AppRepository";

    private final ApiClient apiClient;
    private final ExecutorService executor;

    public AppRepository(Context context) {
        this.apiClient = new ApiClient(context.getApplicationContext());
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Task<App> getLatestApp() {
        TaskCompletionSource<App> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                Request request = apiClient.authenticatedRequest("api/app/findAll")
                        .get()
                        .build();

                JSONArray apps = new JSONArray(apiClient.executeForBody(request));
                if (apps.length() == 0) {
                    throw new IllegalStateException("Nenhuma versao encontrada");
                }

                JSONObject latestJson = apps.getJSONObject(0);
                for (int i = 1; i < apps.length(); i++) {
                    JSONObject current = apps.getJSONObject(i);
                    if (current.optLong("id", 0L) >= latestJson.optLong("id", 0L)) {
                        latestJson = current;
                    }
                }

                App latestApp = new App(
                        latestJson.optString("versao"),
                        latestJson.optString("link"),
                        latestJson.optBoolean("atualizado", true)
                );
                latestApp.setId(latestJson.optLong("id"));
                tcs.setResult(latestApp);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao buscar versao do app", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }
}
