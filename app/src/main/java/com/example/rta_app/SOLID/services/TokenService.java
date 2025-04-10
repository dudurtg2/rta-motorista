package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;

import com.example.rta_app.SOLID.activitys.LoginActivity;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TokenService {
    private static final String TAG = "API";
    private static final String FILE_NAME = "user_data.json";
    private static final String URL_REFRESH_TOKEN = "http://147.79.86.117:10102/auth/refresh-token";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Object fileLock = new Object();

    public TokenService(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor(); // Executor para operações de I/O e rede
    }

    public CompletableFuture<JSONObject> validateAndRefreshToken() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String tokensJson = readFile(FILE_NAME);
                if (tokensJson == null || tokensJson.isEmpty()) {
                    throw new Exception("Arquivo de tokens vazio ou não encontrado");
                }
                JSONObject tokens = new JSONObject(tokensJson);

                String accessToken = tokens.optString("accessToken", null);
                String refreshToken = tokens.optString("refreshToken", null);

                if (accessToken == null || refreshToken == null) {
                    Log.e(TAG, "Access token ou refresh token não encontrados");
                    throw new Exception("Access token ou refresh token não encontrados");
                }

                String[] tokenParts = accessToken.split("\\.");
                if (tokenParts.length < 2) {
                    Log.e(TAG, "Token inválido");
                    throw new Exception("Token inválido");
                }

                // Utiliza URL_SAFE para tokens JWT
                String decodedPayload = new String(Base64.decode(tokenParts[1], Base64.URL_SAFE), StandardCharsets.UTF_8);
                JSONObject payload = new JSONObject(decodedPayload);
                long expiration = payload.getLong("exp") * 1000;

                if (System.currentTimeMillis() >= expiration) {
                    Log.i(TAG, "Token expirado, tentando renová-lo");

                    try {
                        JSONObject newTokens = refreshAccessToken(refreshToken);
                        saveTokens(newTokens.getString("accessToken"), refreshToken);
                        return newTokens;
                    } catch (Exception refreshException) {
                        Log.e(TAG, "Erro ao renovar o token", refreshException);
                        // Remove o arquivo de tokens
                        synchronized (fileLock) {
                            context.deleteFile(FILE_NAME);
                        }
                        Log.d(TAG, "Arquivo de tokens excluído");
                        // Redireciona para a tela de login
                        Intent loginIntent = new Intent(context, LoginActivity.class);
                        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(loginIntent);
                        throw new Exception("Token expirado e falha ao renovar, redirecionado para login");
                    }
                } else {
                    Log.i(TAG, "Token ainda válido");
                    JSONObject validTokenResponse = new JSONObject();
                    validTokenResponse.put("accessToken", accessToken);
                    return validTokenResponse;
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao validar ou renovar o token de acesso", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    private JSONObject refreshAccessToken(String refreshToken) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("refreshToken", refreshToken);

        RequestBody body = RequestBody.create(requestBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(URL_REFRESH_TOKEN)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Erro ao renovar o token: " + response.code());
            }

            String responseBody = response.body().string();
            return new JSONObject(responseBody);
        }
    }

    private void saveTokens(String accessToken, String refreshToken) {
        try {
            synchronized (fileLock) {
                String jsonContent = readFile(FILE_NAME);
                JSONObject jsonObject;
                if (jsonContent != null && !jsonContent.isEmpty()) {
                    jsonObject = new JSONObject(jsonContent);
                } else {
                    jsonObject = new JSONObject();
                }
                jsonObject.put("accessToken", accessToken);
                jsonObject.put("refreshToken", refreshToken);
                writeFile(FILE_NAME, jsonObject.toString());
            }
            Log.d(TAG, "Tokens atualizados com sucesso no arquivo.");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao atualizar os tokens no arquivo", e);
        }
    }

    private String readFile(String fileName) throws Exception {
        synchronized (fileLock) {
            try (FileInputStream fis = context.openFileInput(fileName)) {
                byte[] buffer = new byte[fis.available()];
                int readBytes = fis.read(buffer);
                if (readBytes <= 0) {
                    return "";
                }
                return new String(buffer, StandardCharsets.UTF_8);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao ler o arquivo " + fileName, e);
                throw e;
            }
        }
    }

    private void writeFile(String fileName, String content) throws Exception {
        synchronized (fileLock) {
            try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                Log.e(TAG, "Erro ao escrever no arquivo " + fileName, e);
                throw e;
            }
        }
    }
}
