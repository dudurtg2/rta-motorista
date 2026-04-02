package com.example.rta_app.SOLID.activitys.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.SOLID.api.WorkerHourRepository;
import com.example.rta_app.SOLID.entities.Users;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.services.TokenStorage;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PingWorker extends Worker {
    private static final String TAG = "PingWorker";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build();

    private final UsersRepository usersRepository;
    private final WorkerHourRepository workerHourRepository;
    private final TokenStorage tokenStorage;

    public PingWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        Context appContext = context.getApplicationContext();
        usersRepository = new UsersRepository(appContext);
        workerHourRepository = new WorkerHourRepository(appContext);
        tokenStorage = new TokenStorage(appContext);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String apiKey = tokenStorage.getApiKey();
            if (apiKey.isEmpty()) {
                Log.w(TAG, "doWork(): sem API key; encerrando sem retry");
                return Result.failure();
            }

            Users user = Tasks.await(usersRepository.getUser(), 10, TimeUnit.SECONDS);
            if (user == null || user.getUid() == null || user.getUid().trim().isEmpty()) {
                Log.w(TAG, "doWork(): usuário indisponível");
                return Result.retry();
            }

            WorkerHous workerHous = Tasks.await(workerHourRepository.getWorkerHous(), 10, TimeUnit.SECONDS);
            if (!shouldSendPing(workerHous)) {
                Log.i(TAG, "doWork(): fora da janela válida de rastreamento; ping ignorado sem retry");
                return Result.success();
            }

            int motoristaId;
            try {
                motoristaId = Integer.parseInt(user.getUid().trim());
            } catch (NumberFormatException e) {
                Log.e(TAG, "doWork(): UID do motorista inválido: " + user.getUid(), e);
                return Result.failure();
            }

            Data input = getInputData();
            JSONObject payload = new JSONObject()
                    .put("lat", input.getDouble("lat", 0.0))
                    .put("lon", input.getDouble("lon", 0.0))
                    .put("accuracy", input.getFloat("acc", 0f))
                    .put("speed", input.getFloat("spd", 0f))
                    .put("bearing", input.getFloat("brg", 0f))
                    .put("timestamp", input.getLong("ts", 0L))
                    .put("battery", batteryPct(getApplicationContext()))
                    .put("motorista", new JSONObject().put("id", motoristaId));

            byte[] gzippedBody = gzip(payload.toString().getBytes());
            RequestBody body = RequestBody.create(gzippedBody, JSON_MEDIA);
            Request request = new Request.Builder()
                    .url(LocationTracker.BASE_URL + "/api/locationPings/save")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Content-Encoding", "gzip")
                    .addHeader("X-API-Key", apiKey)
                    .post(body)
                    .build();

            try (Response response = CLIENT.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "doWork(): ping enviado com sucesso");
                    return Result.success();
                }

                int code = response.code();
                Log.w(TAG, "doWork(): falha HTTP=" + code);
                if (code >= 400 && code < 500) {
                    return Result.failure();
                }
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "doWork(): erro ao enviar ping", e);
            return Result.retry();
        }
    }

    private boolean shouldSendPing(WorkerHous workerHous) {
        if (workerHous == null) {
            return false;
        }

        String first = safe(workerHous.getHour_first());
        String dinner = safe(workerHous.getHour_dinner());
        String finish = safe(workerHous.getHour_finish());

        boolean workingBeforeLunch = !first.isEmpty() && dinner.isEmpty();
        boolean workingAfterLunch = !dinner.isEmpty() && finish.isEmpty();
        return workingBeforeLunch || workingAfterLunch;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static int batteryPct(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) {
            return -1;
        }
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) {
            return -1;
        }
        return Math.round(100f * level / scale);
    }

    private static byte[] gzip(byte[] input) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(bos)) {
            gos.write(input);
        }
        return bos.toByteArray();
    }
}
