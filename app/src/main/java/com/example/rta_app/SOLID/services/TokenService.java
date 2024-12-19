package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TokenService {
    private static final String TAG = "TokenService";
    private static final String FILE_NAME = "user_data.json";
    private static final String URL_REFRESH_TOKEN = "http://carlo4664.c44.integrator.host:10500/auth/refresh-token";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private final OkHttpClient httpClient;

    public TokenService(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient();
    }

    public CompletableFuture<JSONObject> validateAndRefreshToken() {
        CompletableFuture<JSONObject> future = new CompletableFuture<>();

        try {
            String tokensJson = readFile(FILE_NAME);
            JSONObject tokens = new JSONObject(tokensJson);

            String accessToken = tokens.optString("accessToken", null);
            String refreshToken = tokens.optString("refreshToken", null);

            if (accessToken == null || refreshToken == null) {
                Log.e(TAG, "Access token ou refresh token não encontrados");
                future.completeExceptionally(new Exception("Access token ou refresh token não encontrados"));
                return future;
            }

            String[] tokenParts = accessToken.split("\\.");
            if (tokenParts.length < 2) {
                Log.e(TAG, "Token inválido");
                future.completeExceptionally(new Exception("Token inválido"));
                return future;
            }

            String decodedPayload = new String(Base64.decode(tokenParts[1], Base64.DEFAULT), StandardCharsets.UTF_8);
            JSONObject payload = new JSONObject(decodedPayload);
            long expiration = payload.getLong("exp") * 1000;

            if (System.currentTimeMillis() >= expiration) {
                Log.i(TAG, "Token expirado, tentando renová-lo");


                JSONObject requestBody = new JSONObject();
                requestBody.put("refreshToken", refreshToken);

                RequestBody body = RequestBody.create(requestBody.toString(), JSON);
                Request request = new Request.Builder()
                        .url(URL_REFRESH_TOKEN)
                        .post(body)
                        .build();

                new Thread(() -> {
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            Log.d(TAG, "Resposta da API: " + responseBody);

                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String newAccessToken = jsonResponse.getString("accessToken");

                            saveTokens(newAccessToken, refreshToken);

                            future.complete(jsonResponse);
                        } else {
                            Log.e(TAG, "Erro ao renovar o token de acesso: " + response.code());
                            future.completeExceptionally(new Exception("Erro ao renovar o token de acesso"));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao chamar a API de renovação do token", e);
                        future.completeExceptionally(e);
                    }
                }).start();
            } else {
                Log.i(TAG, "Token ainda válido");
                JSONObject validTokenResponse = new JSONObject();
                validTokenResponse.put("accessToken", accessToken);
                future.complete(validTokenResponse);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao validar ou renovar o token de acesso", e);
            future.completeExceptionally(e);
        }

        return future;
    }

    private void saveTokens(String accessToken, String refreshToken) {
        try {
            String jsonContent = readFile(FILE_NAME);
            JSONObject jsonObject;

            if (!jsonContent.isEmpty()) {
                jsonObject = new JSONObject(jsonContent);
            } else {
                jsonObject = new JSONObject();
            }

            jsonObject.put("accessToken", accessToken);
            jsonObject.put("refreshToken", refreshToken);

            writeFile(FILE_NAME, jsonObject.toString());
            Log.d(TAG, "Tokens atualizados com sucesso no arquivo.");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao atualizar os tokens no arquivo", e);
        }
    }


    private String readFile(String fileName) throws Exception {
        try (FileInputStream fis = context.openFileInput(fileName)) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }

    private void writeFile(String fileName, String content) throws Exception {
        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
