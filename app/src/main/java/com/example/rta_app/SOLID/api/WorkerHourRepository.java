
package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.services.TokenService;
import com.example.rta_app.SOLID.services.TokenStorage;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WorkerHourRepository{

    private static final String TAG = "WorkerHourRepository";
    private static final String FILE_NAME = "worker_hours.json";
    private static final String USER_FILE = "user_data.json";
    private static final String BASE_URL = "https://android.lc-transportes.com/";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private final TokenService tokenService;
    private final TokenStorage tokenStorage;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;

    public WorkerHourRepository(Context context) {
        this.context = context.getApplicationContext();
        this.tokenService = new TokenService(this.context);
        this.httpClient = new OkHttpClient();
        this.tokenStorage = new TokenStorage(this.context);
        this.executor = Executors.newSingleThreadExecutor();
    }

   
    public Task<Void> validadeCode(String code) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        tokenService.validateAndRefreshToken();

        String token = getAccessToken();
        if (token == null) {
            tcs.setException(new IllegalStateException("Access token missing"));
            return tcs.getTask();
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "api/unique/validade/" + code)
                .put(RequestBody.create(new byte[0], null))
                .addHeader("Authorization", "Bearer " + token)
                .build();

        executeRequest(request, tcs);
        return tcs.getTask();
    }

   
    public Task<Void> saveHors(WorkerHous workerHous) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        tokenService.validateAndRefreshToken();

        String token = getAccessToken();
        String driveId = getUserId();
        if (token == null || driveId == null) {
            tcs.setException(new IllegalStateException("Token or user ID missing"));
            return tcs.getTask();
        }

        JSONObject json = new JSONObject();
        try {
            json.put("date", workerHous.getDate());
            json.put("hour_first", workerHous.getHour_first());
            json.put("hour_dinner", workerHous.getHour_dinner());
            json.put("hour_finish", workerHous.getHour_finish());
            json.put("hour_stop", workerHous.getHour_stop());
        } catch (JSONException e) {
            tcs.setException(e);
            return tcs.getTask();
        }

        RequestBody body = RequestBody.create(json.toString(), JSON_MEDIA);
        Request request = new Request.Builder()
                .url(BASE_URL + "api/pontos/save/" + driveId)
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        executeRequest(request, tcs);
        return tcs.getTask();
    }

   
    public Task<Void> saveWorkerHous(WorkerHous workerHous) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("date", workerHous.getDate());
                json.put("hour_first", workerHous.getHour_first());
                json.put("hour_dinner", workerHous.getHour_dinner());
                json.put("hour_finish", workerHous.getHour_finish());
                json.put("hour_stop", workerHous.getHour_stop());
                json.put("hour_after", workerHous.getHour_after());
                writeToFile(json.toString());
                tcs.setResult(null);
            } catch (Exception e) {
                Log.e(TAG, "Error saving local data", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

   
    public Task<WorkerHous> getWorkerHous() {
        TaskCompletionSource<WorkerHous> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String data = readFromFile();
                if (data == null) {
                    tcs.setResult(new WorkerHous("", "", "", "", "", ""));
                } else {
                    JSONObject json = new JSONObject(data);
                    WorkerHous wh = new WorkerHous(
                            json.optString("date", ""),
                            json.optString("hour_first", ""),
                            json.optString("hour_dinner", ""),
                            json.optString("hour_finish", ""),
                            json.optString("hour_stop", ""),
                            json.optString("hour_after", "")
                    );
                    tcs.setResult(wh);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading local data", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private void executeRequest(Request request, TaskCompletionSource<Void> tcs) {
        executor.execute(() -> {
            try (Response resp = httpClient.newCall(request).execute()) {
                String body = resp.body() != null ? resp.body().string() : "";
                if (resp.isSuccessful()) {
                    Log.d(TAG, "API Success: " + body);
                    tcs.setResult(null);
                } else {
                    Log.e(TAG, "API Failure: " + body);
                    tcs.setException(new IOException("API error: " + body));
                }
            } catch (Exception e) {
                Log.e(TAG, "Request error", e);
                tcs.setException(e);
            }
        });
    }

   
    public void writeToFile(String data) throws IOException {
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(data.getBytes(StandardCharsets.UTF_8));
        }
    }

   
    public String readFromFile() throws IOException {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return null;
        try (FileInputStream fis = context.openFileInput(FILE_NAME)) {
            byte[] buf = new byte[fis.available()];
            fis.read(buf);
            return new String(buf, StandardCharsets.UTF_8);
        }
    }

    private String getAccessToken() {
        tokenService.validateAndRefreshToken();
        return tokenStorage.getAccessToken();
    }

    private String getUserId() {
        try {
            String json = readFile(USER_FILE);
            JSONObject data = new JSONObject(json).getJSONObject("data");
            return data.optString("id", null);
        } catch (Exception e) {
            Log.e(TAG, "User ID read error", e);
            return null;
        }
    }

    private String readFile(String name) throws IOException {
        try (FileInputStream fis = context.openFileInput(name)) {
            byte[] buf = new byte[fis.available()];
            fis.read(buf);
            return new String(buf, StandardCharsets.UTF_8);
        }
    }
}
