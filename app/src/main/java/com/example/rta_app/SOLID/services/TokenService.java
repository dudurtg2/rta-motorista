package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.rta_app.SOLID.activitys.LoginActivity;
import com.example.rta_app.SOLID.services.TokenStorage;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TokenService {
    private static final String TAG               = "TokenService";
    private static final String URL_REFRESH_TOKEN = "http://147.79.86.117:10102/auth/refresh-token";
    private static final String URL_TEST_TOKEN    = "http://147.79.86.117:10102/api/teste/validate";
    private static final MediaType JSON_MEDIA     = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private final TokenStorage storage;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;

    public TokenService(Context context)  {
        this.context    = context.getApplicationContext();
        this.storage    = new TokenStorage(this.context);
        this.httpClient = new OkHttpClient();
        this.executor   = Executors.newSingleThreadExecutor();
        Log.d(TAG, "TokenService initialized");
    }

    /**
     * Valida o accessToken via endpoint e, se inválido, renova usando refreshToken.
     * Em caso de falha, limpa storage e redireciona para LoginActivity.
     */
    public Task<JSONObject> validateAndRefreshToken() {
        TaskCompletionSource<JSONObject> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                JSONObject tokens = storage.getTokens();
                String accessToken  = tokens.getString("accessToken");
                String refreshToken = tokens.getString("refreshToken");

                JSONObject resultTokens = tokens;
                if (!isTokenValid(accessToken)) {
                    Log.i(TAG, "Token inválido, renovando...");
                    resultTokens = refreshAndBuildTokens(refreshToken);
                    storage.saveTokens(
                            resultTokens.getString("accessToken"),
                            resultTokens.getString("refreshToken")
                    );
                } else {
                    Log.i(TAG, "Token válido, mantendo existentes");
                }

                tcs.setResult(resultTokens);

            } catch (Exception e) {
                Log.e(TAG, "Erro ao validar/renovar token", e);
                // limpa tokens e envia para login
                storage.clear();
                new Handler(Looper.getMainLooper()).post(() -> {
                    Intent intent = new Intent(context, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                });
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    // --- Helpers privados ---

    /** Chama o endpoint de validação para verificar se o token ainda é aceito */
    private boolean isTokenValid(String accessToken) {
        Request req = new Request.Builder()
                .url(URL_TEST_TOKEN)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response resp = httpClient.newCall(req).execute()) {
            Log.d(TAG, "ValidateToken → HTTP " + resp.code());
            return resp.isSuccessful();
        } catch (IOException e) {
            Log.e(TAG, "Erro no teste de token", e);
            return false;
        }
    }

    /**
     * Executa o refresh-token no servidor e monta o JSONObject com os novos tokens.
     */
    private JSONObject refreshAndBuildTokens(String refreshToken) throws Exception {
        JSONObject body = new JSONObject().put("refreshToken", refreshToken);
        Request req = new Request.Builder()
                .url(URL_REFRESH_TOKEN)
                .post(RequestBody.create(body.toString(), JSON_MEDIA))
                .build();

        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                throw new IOException("Falha no refresh: HTTP " + resp.code());
            }
            JSONObject json = new JSONObject(resp.body().string());
            String newAccess  = json.getString("accessToken");
            String newRefresh = json.optString("refreshToken", refreshToken);

            JSONObject result = new JSONObject();
            result.put("accessToken", newAccess);
            result.put("refreshToken", newRefresh);
            return result;
        }
    }
}
