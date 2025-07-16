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

public class WorkerHourRepository {

    private static final String TAG = "WorkerHourRepository2";
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
        Log.d(TAG, "Constructor: initializing WorkerHourRepository");
        this.context = context.getApplicationContext();
        this.tokenService = new TokenService(this.context);
        this.httpClient = new OkHttpClient();
        this.tokenStorage = new TokenStorage(this.context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Task<Void> validadeCode(String code) {
        Log.d(TAG, "validadeCode(): code=" + code);
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        tokenService.validateAndRefreshToken();

        String token = getAccessToken();
        if (token == null) {
            Log.e(TAG, "validadeCode(): access token missing");
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
        Log.d(TAG, "saveHors(): workerHous=" + workerHous);
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        tokenService.validateAndRefreshToken();

        String token = getAccessToken();
        String driveId = getUserId();
        if (token == null || driveId == null) {
            Log.e(TAG, "saveHors(): token or userId missing (token=" + token + ", userId=" + driveId + ")");
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
            Log.e(TAG, "saveHors(): JSON error", e);
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
        Log.d(TAG, "saveWorkerHous(): workerHous=" + workerHous);
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
                Log.d(TAG, "saveWorkerHous(): local file saved");
                tcs.setResult(null);
            } catch (Exception e) {
                Log.e(TAG, "saveWorkerHous(): error saving local data", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    public Task<WorkerHous> getWorkerHous() {
        Log.d(TAG, "getWorkerHous(): reading from local file");
        TaskCompletionSource<WorkerHous> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String data = readFromFile();
                if (data == null) {
                    Log.d(TAG, "getWorkerHous(): no file, returning empty WorkerHous");
                    tcs.setResult(new WorkerHous("", "", "", "", "", ""));
                } else {
                    JSONObject json = new JSONObject(data);
                    Log.d(TAG,json.toString());
                    WorkerHous wh = new WorkerHous(
                            json.optString("date", ""),
                            json.optString("hour_first", ""),
                            json.optString("hour_dinner", ""),
                            json.optString("hour_finish", ""),
                            json.optString("hour_stop", ""),
                            json.optString("hour_after", "")
                    );
                    Log.d(TAG, "getWorkerHous(): parsed WorkerHous=" + wh.toString());
                    tcs.setResult(wh);
                }
            } catch (Exception e) {
                Log.e(TAG, "getWorkerHous(): error reading local data", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private void executeRequest(Request request, TaskCompletionSource<Void> tcs) {
        Log.d(TAG, "executeRequest(): url=" + request.url());
        executor.execute(() -> {
            try (Response resp = httpClient.newCall(request).execute()) {
                String body = resp.body() != null ? resp.body().string() : "";
                if (resp.isSuccessful()) {
                    Log.d(TAG, "executeRequest(): success, body=" + body);
                    tcs.setResult(null);
                } else {
                    Log.e(TAG, "executeRequest(): failure, body=" + body);
                    tcs.setException(new IOException("API error: " + body));
                }
            } catch (Exception e) {
                Log.e(TAG, "executeRequest(): request error", e);
                tcs.setException(e);
            }
        });
    }

    public void writeToFile(String data) throws IOException {
        Log.d(TAG, "writeToFile(): writing data length=" + (data != null ? data.length() : 0));
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(data.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String readFromFile() throws IOException {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            Log.d(TAG, "readFromFile(): file not found");
            return null;
        }
        try (FileInputStream fis = context.openFileInput(FILE_NAME)) {
            byte[] buf = new byte[fis.available()];
            fis.read(buf);
            String result = new String(buf, StandardCharsets.UTF_8);
            Log.d(TAG, "readFromFile(): read data length=" + result.length());
            return result;
        }
    }

    private String getAccessToken() {
        Log.d(TAG, "getAccessToken(): refreshing token");
        tokenService.validateAndRefreshToken();
        String token = tokenStorage.getAccessToken();
        Log.d(TAG, "getAccessToken(): token=" + (token != null ? "[REDACTED]" : "null"));
        return token;
    }

    private String getUserId() {
        Log.d(TAG, "getUserId(): reading from " + USER_FILE);
        try {
            String json = readFile(USER_FILE);
            JSONObject data = new JSONObject(json).getJSONObject("data");
            String id = data.optString("id", null);
            Log.d(TAG, "getUserId(): id=" + id);
            return id;
        } catch (Exception e) {
            Log.e(TAG, "getUserId(): error reading user ID", e);
            return null;
        }
    }

    private String readFile(String name) throws IOException {
        Log.d(TAG, "readFile(): name=" + name);
        try (FileInputStream fis = context.openFileInput(name)) {
            byte[] buf = new byte[fis.available()];
            fis.read(buf);
            String result = new String(buf, StandardCharsets.UTF_8);
            Log.d(TAG, "readFile(): read length=" + result.length());
            return result;
        }
    }
}
