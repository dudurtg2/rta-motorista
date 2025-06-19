package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.rta_app.SOLID.activitys.LoginActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TokenService {
    private static final String TAG               = "API_teste";
    private static final String FILE_NAME         = "user_data.json";
    private static final String URL_REFRESH_TOKEN = "http://147.79.86.117:10102/auth/refresh-token";
    private static final String URL_TEST_TOKEN    = "http://147.79.86.117:10102/api/teste/validate";
    private static final MediaType JSON           = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Object fileLock = new Object();

    public TokenService(Context context) {
        this.context    = context;
        this.httpClient = new OkHttpClient();
        this.executor   = Executors.newSingleThreadExecutor();
        Log.d(TAG, "TokenService iniciado");
    }


    public Task<JSONObject> validateAndRefreshToken() {
        TaskCompletionSource<JSONObject> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                Log.d(TAG, "Iniciando validateAndRefreshToken()");

                // 1) Lê tokens salvos
                String tokensJson = readFile(FILE_NAME);
                if (tokensJson == null || tokensJson.isEmpty()) {
                    throw new Exception("Arquivo de tokens vazio ou não encontrado");
                }
                JSONObject stored   = new JSONObject(tokensJson);
                String accessToken  = stored.optString("accessToken", null);
                String refreshToken = stored.optString("refreshToken", null);
                String expiredStr   = stored.optString("tokenExpired", null);
                if (accessToken == null || refreshToken == null || expiredStr == null) {
                    throw new Exception("Tokens ou data de expiração faltando");
                }

                // Parse da data de expiração no fuso de Brasília
                LocalDateTime tokenExpired = LocalDateTime.parse(expiredStr);
                LocalDateTime nowSP       = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
                Log.d(TAG, "tokenExpired=" + tokenExpired + ", nowSP=" + nowSP);

                JSONObject resultTokens = new JSONObject()
                        .put("accessToken", accessToken)
                        .put("refreshToken", refreshToken);

                // Se expirou pelo timestamp, renova sem testar endpoint
                if (tokenExpired.isBefore(nowSP)) {
                    Log.i(TAG, "Token expirado via timestamp, renovando sem teste de endpoint");
                    resultTokens = refreshAccessToken(refreshToken);
                    saveTokens(
                            resultTokens.getString("accessToken"),
                            resultTokens.getString("refreshToken")
                    );
                } else {
                    Log.i(TAG, "Token ainda válido via timestamp, pulando teste de endpoint");
                }

                Log.d(TAG, "validateAndRefreshToken() completado com sucesso");
                tcs.setResult(resultTokens);

            } catch (Exception e) {
                Log.e(TAG, "Erro ao validar/renovar token", e);
                synchronized (fileLock) {
                    context.deleteFile(FILE_NAME);
                    Log.d(TAG, "Arquivo de tokens deletado após erro");
                }
                Intent login = new Intent(context, LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(login);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private JSONObject refreshAccessToken(String refreshToken) throws Exception {
        Log.d(TAG, "refreshAccessToken() iniciado");
        JSONObject bodyJson = new JSONObject().put("refreshToken", refreshToken);
        RequestBody body     = RequestBody.create(bodyJson.toString(), JSON);
        Request request      = new Request.Builder()
                .url(URL_REFRESH_TOKEN)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            Log.d(TAG, "HTTP POST " + URL_REFRESH_TOKEN + " -> " + response.code());
            if (!response.isSuccessful()) {
                throw new Exception("Falha no refresh: HTTP " + response.code());
            }
            String respBody = response.body().string();
            Log.d(TAG, "Corpo da resposta do refresh: " + respBody);
            JSONObject json = new JSONObject(respBody);
            return new JSONObject()
                    .put("accessToken", json.getString("accessToken"))
                    .put("refreshToken", refreshToken)
                    .put("tokenExpired", LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).toString());
        }
    }

    private void saveTokens(String accessToken, String refreshToken) throws Exception {
        Log.d(TAG, "saveTokens() iniciado");
        synchronized (fileLock) {
            JSONObject obj = new JSONObject()
                    .put("accessToken", accessToken)
                    .put("refreshToken", refreshToken);
            writeFile(FILE_NAME, obj.toString());
            Log.d(TAG, "Tokens salvos com sucesso: " + obj.toString());
        }
    }
    private String readFile(String fileName) throws Exception {
        synchronized (fileLock) {
            try (FileInputStream fis = context.openFileInput(fileName)) {
                byte[] buf = new byte[fis.available()];
                int len = fis.read(buf);
                if (len <= 0) return "";
                return new String(buf, StandardCharsets.UTF_8);
            }
        }
    }

    private void writeFile(String fileName, String content) throws Exception {
        synchronized (fileLock) {
            try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
