package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.services.ApiClient;
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

import okhttp3.Request;
import okhttp3.RequestBody;

public class WorkerHourRepository {

    private static final String TAG = "WorkerHourRepository2";
    private static final String FILE_NAME = "worker_hours.json";
    private static final String USER_FILE = "user_data.json";

    private final Context context;
    private final ApiClient apiClient;
    private final ExecutorService executor;

    public WorkerHourRepository(Context context) {
        Log.d(TAG, "Constructor: initializing WorkerHourRepository");
        this.context = context.getApplicationContext();
        this.apiClient = new ApiClient(this.context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Task<Void> saveHors(WorkerHous workerHous) {
        Log.d(TAG, "saveHors(): workerHous=" + workerHous);
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

        executor.execute(() -> {
            try {
                String driveId = getUserIdOrThrow();
                JSONObject json = buildRemoteWorkerJson(workerHous);
                RequestBody body = RequestBody.create(json.toString(), ApiClient.JSON_MEDIA);
                Request request = apiClient.authenticatedRequest("api/pontos/save/" + driveId)
                        .post(body)
                        .build();

                executeRequest(request);
                tcs.setResult(null);
            } catch (Exception e) {
                Log.e(TAG, "saveHors(): request error", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    public Task<Void> saveWorkerHous(WorkerHous workerHous) {
        Log.d(TAG, "saveWorkerHous(): workerHous=" + workerHous);
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                JSONObject json = buildLocalWorkerJson(workerHous);
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
                    tcs.setResult(new WorkerHous("", "", "", "", "", "", "", "", "", "", "", "", "", ""));
                    return;
                }

                JSONObject json = new JSONObject(data);
                Log.d(TAG, json.toString());
                WorkerHous wh = new WorkerHous(
                        json.optString("date", ""),
                        json.optString("hour_first", ""),
                        json.optString("hour_dinner", ""),
                        json.optString("hour_finish", ""),
                        json.optString("hour_stop", ""),
                        json.optString("hour_after", ""),
                        json.optString("latitude_first", ""),
                        json.optString("longitude_first", ""),
                        json.optString("latitude_dinner", ""),
                        json.optString("longitude_dinner", ""),
                        json.optString("latitude_stop", ""),
                        json.optString("longitude_stop", ""),
                        json.optString("latitude_finish", ""),
                        json.optString("longitude_finish", ""),
                        json.optBoolean("carro_inicial", false),
                        json.optBoolean("carro_final", false),
                        json.optLong("id_verificardor", 0L),
                        json.optLong("id_carro", 0L)
                );
                Log.d(TAG, "getWorkerHous(): parsed WorkerHous=" + wh);
                tcs.setResult(wh);
            } catch (Exception e) {
                Log.e(TAG, "getWorkerHous(): error reading local data", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private JSONObject buildRemoteWorkerJson(WorkerHous workerHous) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("date", workerHous.getDate());
        json.put("hour_first", workerHous.getHour_first());
        json.put("hour_dinner", workerHous.getHour_dinner());
        json.put("hour_finish", workerHous.getHour_finish());
        json.put("hour_stop", workerHous.getHour_stop());
        json.put("latitude_first", workerHous.getLatitude_first());
        json.put("longitude_first", workerHous.getLongitude_first());
        json.put("latitude_dinner", workerHous.getLatitude_dinner());
        json.put("longitude_dinner", workerHous.getLongitude_dinner());
        json.put("latitude_stop", workerHous.getLatitude_stop());
        json.put("longitude_stop", workerHous.getLongitude_stop());
        json.put("latitude_finish", workerHous.getLatitude_finish());
        json.put("longitude_finish", workerHous.getLongitude_finish());
        return json;
    }

    private JSONObject buildLocalWorkerJson(WorkerHous workerHous) throws JSONException {
        JSONObject json = buildRemoteWorkerJson(workerHous);
        json.put("hour_after", workerHous.getHour_after());
        json.put("carro_inicial", workerHous.getCarroInicial());
        json.put("carro_final", workerHous.getCarroFinal());
        json.put("id_verificardor", workerHous.getIdVerificardor());
        json.put("id_carro", workerHous.getIdCarro());
        return json;
    }

    private void executeRequest(Request request) throws IOException {
        Log.d(TAG, "executeRequest(): url=" + request.url());
        String body = apiClient.executeForBody(request);
        Log.d(TAG, "executeRequest(): success, body=" + body);
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

    private String getUserIdOrThrow() throws IOException, JSONException {
        Log.d(TAG, "getUserId(): reading from " + USER_FILE);
        String json = readFile(USER_FILE);
        JSONObject data = new JSONObject(json).getJSONObject("data");
        String id = data.optString("id", null);
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalStateException("Token or user ID missing");
        }
        Log.d(TAG, "getUserId(): id=" + id);
        return id;
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
