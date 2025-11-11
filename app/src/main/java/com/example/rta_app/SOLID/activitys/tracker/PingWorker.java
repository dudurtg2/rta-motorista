package com.example.rta_app.SOLID.activitys.tracker;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.rta_app.SOLID.activitys.WorkHourActivity;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.SOLID.services.TokenStorage;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

public class PingWorker extends Worker {
    private static final boolean USE_GZIP_REQUEST = false; // mude para true se sua API suportar
    private UsersRepository usersRepository;
    public PingWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.usersRepository = new UsersRepository(context);
    }
    private void getUser() {
        usersRepository.getUser()
                .addOnSuccessListener(users -> {
                   
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao obter usuário: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        // Auth via header:
        TokenStorage ts = new TokenStorage(ctx);
        String deviceToken = ts.getApiKey();
        if (deviceToken == null || deviceToken.isEmpty()) {
            return Result.retry();
        }


        int motoristaId;


        if (motoristaId <= 0) {
            return Result.retry(); // exige motorista definido antes de enviar
        }

        Data d = getInputData();
        double lat = d.getDouble("lat", 0.0);
        double lon = d.getDouble("lon", 0.0);
        float acc  = d.getFloat("acc", 0f);
        float spd  = d.getFloat("spd", 0f);
        float brg  = d.getFloat("brg", 0f);
        long tsMs  = d.getLong("ts", 0L);
        int  bat   = batteryPct(ctx);

        // Corpo com motorista{id: <int>}
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

        final boolean USE_GZIP_REQUEST = false; // deixe false se sua API não aceita gzip

        try {
            OkHttpClient client = new OkHttpClient();

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    USE_GZIP_REQUEST ? gzip(json.getBytes()) : json.getBytes()
            );

            Request.Builder rb = new Request.Builder()
                    .url(LocationTracker.BASE_URL + "/api/locations")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-API-Key", deviceToken); // AUTH via header

            if (USE_GZIP_REQUEST) rb.addHeader("Content-Encoding", "gzip");

            Request req = rb.post(body).build();
            Response r = client.newCall(req).execute();
            boolean ok = r.isSuccessful();
            r.close();
            return ok ? Result.success() : Result.retry();
        } catch (Exception e) {
            return Result.retry();
        }
    }


    private static int batteryPct(Context ctx) {
        Intent i = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return -1;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) return -1;
        return Math.round(100f * level / scale);
    }

    private static byte[] gzip(byte[] bytes) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gos = new GZIPOutputStream(bos);
        gos.write(bytes);
        gos.close();
        return bos.toByteArray();
    }
}
