package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final int MAX_ERROR_BODY_LOG_LENGTH = 300;

    public static final String BASE_URL = "https://android.lc-transportes.com/v3/";
    public static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient SHARED_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            .build();

    private final TokenStorage tokenStorage;

    public ApiClient(@NonNull Context context) {
        this.tokenStorage = new TokenStorage(context.getApplicationContext());
    }

    public Request.Builder request(@NonNull String path) {
        return new Request.Builder()
                .url(resolveUrl(path))
                .addHeader("Accept", "application/json");
    }

    public Request.Builder authenticatedRequest(@NonNull String path) {
        String apiKey = tokenStorage.getApiKey().trim();
        if (apiKey.isEmpty()) {
            Log.w(TAG, "API key ausente para request autenticada: " + path);
            throw new IllegalStateException("API key ausente. Faca login novamente.");
        }

        return request(path).addHeader("X-API-Key", apiKey);
    }

    public String executeForBody(@NonNull Request request) throws IOException {
        long startNanos = System.nanoTime();
        Log.d(TAG, "HTTP -> " + request.method() + " " + request.url());

        try (Response response = SHARED_CLIENT.newCall(request).execute()) {
            String body = readBody(response);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            logResponse(request, response, elapsedMs, body);
            return requireSuccessfulBody(response, body);
        } catch (IOException e) {
            if (!(e instanceof ApiException)) {
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                Log.e(TAG, "HTTP !! " + request.method() + " " + request.url()
                        + " (" + elapsedMs + " ms): " + e.getMessage(), e);
            }
            throw e;
        }
    }

    public void executeForNoBody(@NonNull Request request) throws IOException {
        executeForBody(request);
    }

    private static String requireSuccessfulBody(Response response, String body) throws IOException {
        if (!response.isSuccessful()) {
            throw new ApiException(response.code(), response.message(), body);
        }
        return body;
    }

    private static String readBody(Response response) throws IOException {
        ResponseBody responseBody = response.body();
        return responseBody == null ? "" : responseBody.string();
    }

    private static void logResponse(Request request, Response response, long elapsedMs, String body) {
        int bodyLength = body == null ? 0 : body.length();
        String message = "HTTP <- " + response.code() + " " + request.method() + " " + request.url()
                + " (" + elapsedMs + " ms, body=" + bodyLength + " chars)";

        if (response.isSuccessful()) {
            Log.i(TAG, message);
            return;
        }

        String preview = bodyPreview(body);
        if (preview.isEmpty()) {
            Log.w(TAG, message);
        } else {
            Log.w(TAG, message + " body=" + preview);
        }
    }

    private static String bodyPreview(String body) {
        if (body == null) {
            return "";
        }

        String preview = body.replace('\n', ' ').replace('\r', ' ').trim();
        if (preview.length() > MAX_ERROR_BODY_LOG_LENGTH) {
            return preview.substring(0, MAX_ERROR_BODY_LOG_LENGTH) + "...";
        }
        return preview;
    }

    private static String resolveUrl(String path) {
        String safePath = path == null ? "" : path.trim();
        if (safePath.startsWith("http://") || safePath.startsWith("https://")) {
            return safePath;
        }
        while (safePath.startsWith("/")) {
            safePath = safePath.substring(1);
        }
        if (BASE_URL.endsWith("/") || safePath.isEmpty()) {
            return BASE_URL + safePath;
        }
        return BASE_URL + "/" + safePath;
    }
}
