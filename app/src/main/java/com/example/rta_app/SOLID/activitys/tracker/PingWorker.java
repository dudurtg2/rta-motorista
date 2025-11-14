package com.example.rta_app.SOLID.activitys.tracker;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.SOLID.api.WorkerHourRepository;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.services.TokenStorage;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

public class PingWorker extends Worker {
    private static final String TAG = "PingWorker";

    // Reuso de conexão (keep-alive)
    private static final OkHttpClient CLIENT = new OkHttpClient();

    private UsersRepository usersRepository;
    private WorkerHourRepository workerHourRepository;
    private int motoristaId;
    private boolean isValidate;

    public PingWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        Log.d(TAG, "constructor: init repositories");
        this.usersRepository = new UsersRepository(context);
        this.workerHourRepository = new WorkerHourRepository(context);
        getUser();
    }

    private void getUser() {
        Log.d(TAG, "getUser(): iniciando fetch de usuário e worker hours");

        usersRepository.getUser()
                .addOnSuccessListener(users -> {
                    try {
                        motoristaId = Integer.parseInt(users.getUid());
                        Log.i(TAG, "getUser(): motoristaId definido = " + motoristaId);
                    } catch (NumberFormatException nfe) {
                        Log.e(TAG, "getUser(): UID não é int: " + users.getUid(), nfe);
                        motoristaId = 0;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getUser(): falha ao obter usuário", e);
                    motoristaId = 0;
                });

        workerHourRepository.getWorkerHous().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WorkerHous workerHous = task.getResult();

                if(!workerHous.getHour_first().isEmpty() && workerHous.getHour_dinner().isEmpty()){
                    isValidate = true;
                } else if (!workerHous.getHour_dinner().isEmpty() && !workerHous.getHour_finish().isEmpty()){
                    isValidate = true;
                } else {
                    isValidate = false;
                }

                Log.i(TAG, "getUser(): isValidate=" + isValidate);
            } else {
                isValidate = false;
                Log.e(TAG, "getUser(): falha ao obter workerHous", task.getException());
            }
        });
    }

    @Override
    public Result doWork() {
        Log.d(TAG, "doWork(): start");

        Context ctx = getApplicationContext();

        TokenStorage ts = new TokenStorage(ctx);
        String deviceToken = ts.getApiKey();
        Log.d(TAG, "doWork(): token presente? " + (deviceToken != null && !deviceToken.isEmpty()));
        if (deviceToken == null || deviceToken.isEmpty()) {
            Log.w(TAG, "doWork(): sem deviceToken -> retry");
            return Result.retry();
        }

        Log.d(TAG, "doWork(): motoristaId=" + motoristaId + " | isValidate=" + isValidate);
        if (motoristaId <= 0) {
            Log.w(TAG, "doWork(): motoristaId inválido -> retry");
            return Result.retry();
        }

        Data d = getInputData();
        double lat = d.getDouble("lat", 0.0);
        double lon = d.getDouble("lon", 0.0);
        float acc  = d.getFloat("acc", 0f);
        float spd  = d.getFloat("spd", 0f);
        float brg  = d.getFloat("brg", 0f);
        long tsMs  = d.getLong("ts", 0L);
        int  bat   = batteryPct(ctx);

        Log.d(TAG, String.format("doWork(): ponto lat=%.6f lon=%.6f acc=%.2f spd=%.2f brg=%.2f ts=%d bat=%d",
                lat, lon, acc, spd, brg, tsMs, bat));

        String json = "{"
                + "\"lat\":" + lat + ","
                + "\"lon\":" + lon + ","
                + "\"accuracy\":" + acc + ","
                + "\"speed\":" + spd + ","
                + "\"bearing\":" + brg + ","
                + "\"timestamp\":" + tsMs + ","
                + "\"battery\":" + bat + ","
                + "\"motorista\":{\"id\":" + motoristaId + "}"
                + "}";

        try {
            if (!isValidate) {
                Log.w(TAG, "doWork(): isValidate=false (sem workerHous) -> retry");
                return Result.retry();
            }

            byte[] gzBody = gzip(json.getBytes());
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), gzBody);

            String url = LocationTracker.BASE_URL + "/api/locationPings/save";
            Log.d(TAG, "doWork(): POST " + url + " | gzip=true (body)");

            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Content-Encoding", "gzip")   // << sempre gzip
                    .addHeader("X-API-Key", deviceToken)     // auth
                    .post(body)
                    .build();

            Response r = CLIENT.newCall(req).execute();
            int code = r.code();
            String msg = r.message();
            Log.i(TAG, "doWork(): resposta HTTP code=" + code + " msg=" + msg);
            boolean ok = r.isSuccessful();
            r.close();

            if (ok) {
                Log.d(TAG, "doWork(): sucesso");
                return Result.success();
            } else {
                Log.w(TAG, "doWork(): falha HTTP -> retry");
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "doWork(): exceção ao enviar ping", e);
            return Result.retry();
        }
    }

    private static int batteryPct(Context ctx) {
        Intent i = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return -1;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) return -1;
        int pct = Math.round(100f * level / scale);
        Log.d(TAG, "batteryPct(): " + pct + "%");
        return pct;
    }

    private static byte[] gzip(byte[] bytes) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gos = new GZIPOutputStream(bos);
        gos.write(bytes);
        gos.close();
        byte[] out = bos.toByteArray();
        Log.d(TAG, "gzip(): in=" + bytes.length + " bytes, out=" + out.length + " bytes");
        return out;
    }
}
