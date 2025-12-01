package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.entities.VerificadoresDoCarro;
import com.example.rta_app.SOLID.entities.WorkerHous;
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
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VerificardorDeCarroRepository {

    private static final String TAG = "VerificardorDeCarroRepositoryaa";
    private static final String USER_FILE = "user_data.json";
    private static final String BASE_URL = "https://android.lc-transportes.com/";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private final TokenStorage tokenStorage;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;

    public VerificardorDeCarroRepository(Context context) {
        Log.d(TAG, "Constructor: initializing WorkerHourRepository");
        this.context = context.getApplicationContext();
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)   // tempo para conectar
                .writeTimeout(60, TimeUnit.SECONDS)     // tempo para enviar o body (upload)
                .readTimeout(60, TimeUnit.SECONDS)      // tempo esperando a resposta
                .callTimeout(90, TimeUnit.SECONDS)      // tempo total da chamada
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();
        this.tokenStorage = new TokenStorage(this.context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Task<Long> update(VerificadoresDoCarro verificadoresDoCarro, Long id) {
        Log.d(TAG, "saveHors(): verificadoresDoCarro=" + verificadoresDoCarro);
        TaskCompletionSource<Long> tcs = new TaskCompletionSource<>();

        String token = getAccessToken();
        String driveId = getUserId();
        if (token == null || driveId == null) {
            Log.e(TAG, "saveHors(): token or userId missing (token=" + token + ", userId=" + driveId + ")");
            tcs.setException(new IllegalStateException("Token or user ID missing"));
            return tcs.getTask();
        }

        JSONObject json = new JSONObject();

        try {
            json.put("status", verificadoresDoCarro.getStatus());
            json.put("verificadorInicial", verificadoresDoCarro.getVerificadorInicial());
            json.put("verificadorFinal", verificadoresDoCarro.getVerificadorFinal());

            // Datas no formato ISO-8601 (String). Ajuste se seus getters retornarem Date/LocalDateTime.
            json.put("dataInicial", verificadoresDoCarro.getDataInicial());
            json.put("dataFinal", verificadoresDoCarro.getDataFinal());

            json.put("finalizado", verificadoresDoCarro.getFinalizado());

            json.put("combustivelInicial", verificadoresDoCarro.getCombustivelInicial());
            json.put("combustivelFinal", verificadoresDoCarro.getCombustivelFinal());

            json.put("parabrisaInicio", verificadoresDoCarro.getParabrisaInicio());
            json.put("parabrisaFinal", verificadoresDoCarro.getParabrisaFinal());

            JSONObject motoristaJson = new JSONObject();
            motoristaJson.put("id", driveId); // ou verificadoresDoCarro.getMotorista().getId()
            json.put("motorista", motoristaJson);

            json.put("latariaFinal", verificadoresDoCarro.getLatariaFinal());
            json.put("kilometragemFinal", verificadoresDoCarro.getKilometragemFinal());

            json.put("observacoesAdicionaisInicio", verificadoresDoCarro.getObservacoesAdicionaisInicio());
            json.put("latariaInicio", verificadoresDoCarro.getLatariaInicio());
            json.put("kilometragemInicio", verificadoresDoCarro.getKilometragemInicio());
            json.put("observacoesAdicionaisFinal", verificadoresDoCarro.getObservacoesAdicionaisFinal());

        } catch (JSONException e) {
            Log.e(TAG, "saveHors(): JSON error", e);
            tcs.setException(e);
            return tcs.getTask();
        }

        Log.d(TAG, "saveHors(): json=" + json.toString());

        RequestBody body = RequestBody.create(json.toString(), JSON_MEDIA);
        Request request = new Request.Builder()
                .url(BASE_URL + "api/verificadoresDoCarros/finalizar/"+id)
                .put(body)
                .addHeader("X-API-Key", token)
                .build();

        executeRequestReturningId(request, tcs);
        return tcs.getTask();
    }


    public Task<Long> save(VerificadoresDoCarro verificadoresDoCarro) {
        Log.d(TAG, "save(): verificadoresDoCarro=" + verificadoresDoCarro);
        TaskCompletionSource<Long> tcs = new TaskCompletionSource<>();

        String token = getAccessToken();
        String driveId = getUserId();
        if (token == null || driveId == null) {
            Log.e(TAG, "save(): token or userId missing (token=" + token + ", userId=" + driveId + ")");
            tcs.setException(new IllegalStateException("Token or user ID missing"));
            return tcs.getTask();
        }

        JSONObject json = new JSONObject();

        try {
            json.put("status", verificadoresDoCarro.getStatus());
            json.put("verificadorInicial", verificadoresDoCarro.getVerificadorInicial());
            json.put("verificadorFinal", verificadoresDoCarro.getVerificadorFinal());

            json.put("dataInicial", verificadoresDoCarro.getDataInicial());
            json.put("dataFinal", verificadoresDoCarro.getDataFinal());

            json.put("finalizado", verificadoresDoCarro.getFinalizado());

            json.put("combustivelInicial", verificadoresDoCarro.getCombustivelInicial());
            json.put("combustivelFinal", verificadoresDoCarro.getCombustivelFinal());

            json.put("parabrisaInicio", verificadoresDoCarro.getParabrisaInicio());
            json.put("parabrisaFinal", verificadoresDoCarro.getParabrisaFinal());

            // Objeto carro
            JSONObject carroJson = new JSONObject();
            carroJson.put("id", verificadoresDoCarro.getCarro());
            json.put("carro", carroJson);

            // Objeto motorista
            JSONObject motoristaJson = new JSONObject();
            motoristaJson.put("id", driveId);
            json.put("motorista", motoristaJson);

            json.put("latariaFinal", verificadoresDoCarro.getLatariaFinal());
            json.put("kilometragemFinal", verificadoresDoCarro.getKilometragemFinal());

            json.put("observacoesAdicionaisInicio", verificadoresDoCarro.getObservacoesAdicionaisInicio());
            json.put("latariaInicio", verificadoresDoCarro.getLatariaInicio());
            json.put("kilometragemInicio", verificadoresDoCarro.getKilometragemInicio());
            json.put("observacoesAdicionaisFinal", verificadoresDoCarro.getObservacoesAdicionaisFinal());

        } catch (JSONException e) {
            Log.e(TAG, "save(): JSON error", e);
            tcs.setException(e);
            return tcs.getTask();
        }

        Log.d(TAG, "save(): json=" + json.toString());

        RequestBody body = RequestBody.create(json.toString(), JSON_MEDIA);
        Request request = new Request.Builder()
                .url(BASE_URL + "api/verificadoresDoCarros/save")
                .post(body)
                .addHeader("X-API-Key", token)
                .build();

        // NOVO: usa helper que retorna ID
        executeRequestReturningId(request, tcs);

        return tcs.getTask();
    }
    private void executeRequestReturningId(Request request, TaskCompletionSource<Long> tcs) {
        Log.d(TAG, "executeRequestReturningId(): url=" + request.url());
        executor.execute(() -> {
            try (Response resp = httpClient.newCall(request).execute()) {
                String body = resp.body() != null ? resp.body().string() : "";
                Log.d(TAG, "executeRequestReturningId(): response code=" + resp.code() + ", body=" + body);

                if (resp.isSuccessful()) {
                    try {
                        // Supondo que a API retorna algo como:
                        // { "id": 123, "status": "...", ... }
                        JSONObject json = new JSONObject(body);
                        if (!json.has("id")) {
                            Log.e(TAG, "executeRequestReturningId(): 'id' not found in body");
                            tcs.setException(new JSONException("Campo 'id' não encontrado no retorno: " + body));
                            return;
                        }

                        long id = json.getLong("id");
                        Log.d(TAG, "executeRequestReturningId(): parsed id=" + id);
                        tcs.setResult(id);

                    } catch (JSONException e) {
                        Log.e(TAG, "executeRequestReturningId(): JSON parse error", e);
                        tcs.setException(e);
                    }
                } else {
                    Log.e(TAG, "executeRequestReturningId(): failure, code=" + resp.code() + ", body=" + body);
                    tcs.setException(new IOException("API error: " + resp.code() + " - " + body));
                }
            } catch (Exception e) {
                Log.e(TAG, "executeRequestReturningId(): request error", e);
                tcs.setException(e);
            }
        });
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



    private String getAccessToken() {
      
        String token = tokenStorage.getApiKey();
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
