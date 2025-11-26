package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.entities.Carro;
import com.example.rta_app.SOLID.services.TokenStorage;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CarroRotaRepository {
    private static final String TAG = "CarroRepository";
    private static final String URL_API = "https://android.lc-transportes.com/";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final TokenStorage tokenStorage;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;

    public CarroRotaRepository(Context context) {
        this.httpClient = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();
        this.tokenStorage = new TokenStorage(context.getApplicationContext());
    }

    // ============================================================================================
    // MÉTODOS PÚBLICOS (CRUD)
    // ============================================================================================

    /** GET: /api/carros/findAll */
    /** GET: /api/carros/findAll */
    public Task<List<Carro>> findAll() {
        TaskCompletionSource<List<Carro>> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String token = tokenStorage.getApiKey();

                Request req = new Request.Builder()
                        .url(URL_API + "api/carros/findAllAndroid") // ajuste esse path se o endpoint for diferente
                        .addHeader("X-API-Key", token)
                        .get()
                        .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful()) {
                        throw new IOException("HTTP " + resp.code() + " - " + resp.message());
                    }

                    String body = resp.body() != null ? resp.body().string() : "[]";

                    // A API retorna um ARRAY na raiz, igual ao JSON que você mandou
                    JSONArray arr = new JSONArray(body);
                    List<Carro> list = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        list.add(jsonToCarro(obj));
                    }

                    tcs.setResult(list);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro findAll", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    /** GET: /api/carros/findById/{id} */
    public Task<Carro> findById(long id) {
        TaskCompletionSource<Carro> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String token = tokenStorage.getApiKey();
                Request req = new Request.Builder()
                        .url(URL_API + "api/carros/findById/" + id)
                        .addHeader("X-API-Key", token)
                        .get()
                        .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code());

                    String body = resp.body().string();
                    JSONObject obj = new JSONObject(body);
                    tcs.setResult(jsonToCarro(obj));
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro findById", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    /** POST: /api/carros/save */
    public Task<Void> save(Carro carro) {
        return sendRequest("api/carros/save", "POST", carroToJson(carro));
    }

    /** PUT: /api/carros/update/{id} */
    public Task<Void> update(Carro carro) {
        // O ID vai na URL conforme especificado
        return sendRequest("api/carros/update/" + carro.getId(), "PUT", carroToJson(carro));
    }

    /** DELETE: /api/carros/deleteById/{id} */
    public Task<Void> delete(long id) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String token = tokenStorage.getApiKey();
                Request req = new Request.Builder()
                        .url(URL_API + "api/carros/deleteById/" + id)
                        .addHeader("X-API-Key", token)
                        .delete()
                        .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code());
                    tcs.setResult(null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro delete", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    // ============================================================================================
    // HELPERS
    // ============================================================================================

    /** Envia requisições POST ou PUT com Body */
    private Task<Void> sendRequest(String path, String method, JSONObject body) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String token = tokenStorage.getApiKey();
                RequestBody rb = RequestBody.create(body.toString(), JSON_MEDIA);

                Request.Builder builder = new Request.Builder()
                        .url(URL_API + path)
                        .addHeader("X-API-Key", token);

                Request req = method.equalsIgnoreCase("POST") ? builder.post(rb).build() : builder.put(rb).build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful()) {
                        tcs.setResult(null);
                    } else {
                        throw new IOException("HTTP Error: " + resp.code() + " - " + resp.message());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro sendRequest: " + path, e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    /** Converte JSONObject recebido da API para a entidade Carro */
    private Carro jsonToCarro(JSONObject obj) {
        Carro c = new Carro();
        // Usa optLong/optString para evitar crash se o campo vier nulo
        c.setId(obj.optLong("id"));
        c.setPlaca(obj.optString("placa"));
        c.setMarca(obj.optString("marca"));
        c.setModelo(obj.optString("modelo"));
        c.setTipo(obj.optString("tipo"));
        c.setCor(obj.optString("cor"));
        c.setStatus(obj.optString("status"));
        return c;
    }

    /** Converte entidade Carro para JSONObject para enviar à API */
    private JSONObject carroToJson(Carro c) {
        JSONObject json = new JSONObject();
        try {
            // Não enviamos o ID no body para o save, geralmente o backend gera
            // Mas se for update, o ID vai na URL. Se precisar no body, descomente abaixo:
            json.put("id", c.getId());

            json.put("placa", c.getPlaca());
            json.put("marca", c.getMarca());
            json.put("modelo", c.getModelo());
            json.put("tipo", c.getTipo());
            json.put("cor", c.getCor());
            json.put("status", c.getStatus());

            // Nota: O JSON de exemplo tem "motorista" e "imagens".
            // Como sua classe Java Carro não tem esses campos, não estou enviando.
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao criar JSON do carro", e);
        }
        return json;
    }
}