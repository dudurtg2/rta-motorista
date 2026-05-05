package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.entities.Carro;
import com.example.rta_app.SOLID.services.ApiClient;
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

import okhttp3.Request;
import okhttp3.RequestBody;

public class CarroRotaRepository {
    private static final String TAG = "CarroRepository";

    private final ApiClient apiClient;
    private final ExecutorService executor;

    public CarroRotaRepository(Context context) {
        this.apiClient = new ApiClient(context.getApplicationContext());
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Task<List<Carro>> findAll() {
        TaskCompletionSource<List<Carro>> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                Request req = apiClient.authenticatedRequest("api/carros/findAllAndroid")
                        .get()
                        .build();

                String body = apiClient.executeForBody(req);
                JSONArray arr = new JSONArray(body.isEmpty() ? "[]" : body);
                List<Carro> list = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    list.add(jsonToCarro(arr.getJSONObject(i)));
                }

                tcs.setResult(list);
            } catch (Exception e) {
                Log.e(TAG, "Erro findAll", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    public Task<Carro> findById(long id) {
        TaskCompletionSource<Carro> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                Request req = apiClient.authenticatedRequest("api/carros/findById/" + id)
                        .get()
                        .build();

                JSONObject obj = new JSONObject(apiClient.executeForBody(req));
                tcs.setResult(jsonToCarro(obj));
            } catch (Exception e) {
                Log.e(TAG, "Erro findById", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    public Task<Void> save(Carro carro) {
        return sendRequest("api/carros/save", "POST", carroToJson(carro));
    }

    public Task<Void> update(Carro carro) {
        return sendRequest("api/carros/update/" + carro.getId(), "PUT", carroToJson(carro));
    }

    public Task<Void> delete(long id) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                Request req = apiClient.authenticatedRequest("api/carros/deleteById/" + id)
                        .delete()
                        .build();

                apiClient.executeForNoBody(req);
                tcs.setResult(null);
            } catch (Exception e) {
                Log.e(TAG, "Erro delete", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private Task<Void> sendRequest(String path, String method, JSONObject body) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                executeRequest(path, method, body);
                tcs.setResult(null);
            } catch (Exception e) {
                Log.e(TAG, "Erro sendRequest: " + path, e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private void executeRequest(String path, String method, JSONObject body) throws IOException {
        RequestBody rb = RequestBody.create(body.toString(), ApiClient.JSON_MEDIA);
        Request.Builder builder = apiClient.authenticatedRequest(path);
        Request req = method.equalsIgnoreCase("POST") ? builder.post(rb).build() : builder.put(rb).build();
        apiClient.executeForNoBody(req);
    }

    private Carro jsonToCarro(JSONObject obj) {
        Carro c = new Carro();
        c.setId(obj.optLong("id"));
        c.setPlaca(obj.optString("placa"));
        c.setMarca(obj.optString("marca"));
        c.setModelo(obj.optString("modelo"));
        c.setTipo(obj.optString("tipo"));
        c.setCor(obj.optString("cor"));
        c.setStatus(obj.optString("status"));
        return c;
    }

    private JSONObject carroToJson(Carro c) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", c.getId());
            json.put("placa", c.getPlaca());
            json.put("marca", c.getMarca());
            json.put("modelo", c.getModelo());
            json.put("tipo", c.getTipo());
            json.put("cor", c.getCor());
            json.put("status", c.getStatus());
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao criar JSON do carro", e);
        }
        return json;
    }
}
