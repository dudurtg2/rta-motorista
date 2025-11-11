package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.entities.Coletas;
import com.example.rta_app.SOLID.entities.Packet;
import com.example.rta_app.SOLID.services.TokenStorage;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PackingRepository {
    private static final String TAG = "PackingRepo";
    private static final String URL_API = "https://android.lc-transportes.com/";
    private static final String URL_API_GET = "https://android.lc-transportes.com/";
    private static final String FILE_NAME = "user_data.json";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private final TokenStorage tokenStorage;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;

    public PackingRepository(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();
        this.tokenStorage = new TokenStorage(this.context);
    }

    /** Retorna lista de coletas ainda não coletadas */
    public Task<List<Coletas>> colectPack() {
        return fetchList("api/devolucao/findByMotoristaNotColect/", Coletas.class);
    }

    /** Marca código como coletado */
    public Task<Void> postPacked(String codigo) {
        JSONObject body = new JSONObject();
        try {
            body.put("codigo", codigo);
            body.put("coletado", true);
        } catch (JSONException e) {
            TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
            tcs.setException(e);
            return tcs.getTask();
        }
        return sendRequest("api/devolucao/save/" + getIdDrive(), "POST", body);
    }

    /** Retorna lista de pacotes já coletados */
    public Task<List<Packet>> getListPacking() {
        return fetchList("api/devolucao/findByMotorista/", Packet.class);
    }

    // --- Helpers ---

    private <T> Task<List<T>> fetchList(String path, Class<T> type) {
        TaskCompletionSource<List<T>> tcs = new TaskCompletionSource<>();

        // 1) Primeiro valide/refresh o token de forma assíncrona
       
                    String accessToken;
                    try {
                        accessToken = tokenStorage.getApiKey();
                    } catch (Exception e) {
                        tcs.setException(e);
                        return null;
                    }

                    // 2) Só depois disso, faça o HTTP numa thread de background
                    executor.execute(() -> {
                        try {
                            String driverId = getIdDrive();
                            if (driverId == null || driverId.isEmpty()) {
                                throw new IllegalStateException("Driver ID inválido");
                            }

                            Request req = new Request.Builder()
                                    .url(URL_API + path + driverId)
                                    .addHeader("X-API-Key", accessToken)
                                    .addHeader("Accept", "application/json")
                                    .get()
                                    .build();

                            try (Response resp = httpClient.newCall(req).execute()) {
                                if (!resp.isSuccessful()) {
                                    throw new IOException("HTTP " + resp.code());
                                }

                                String body = resp.body().string();
                                JSONArray arr = new JSONArray(body);
                                List<T> list = new ArrayList<>();
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject obj = arr.getJSONObject(i);
                                    if (type == Coletas.class) {
                                        Coletas c = new Coletas();
                                        c.setEntregador(obj.optJSONObject("entregador").optString("nome"));
                                        c.setCodigos(obj.optString("codigos"));
                                        c.setQtd(obj.optString("quantidade"));
                                        list.add(type.cast(c));

                                    } else { // Packet.class
                                        Packet p = new Packet();
                                        p.setEntregador(obj.optJSONObject("entregador").optString("nome"));
                                        p.setCodigo(obj.optJSONObject("codigo").optString("codigo"));
                                        p.setData(
                                                obj.optString("dataDevolvido")
                                                        .replace("T", " ")
                                                        .split("\\.")[0]
                                        );
                                        p.setRta(
                                                obj.optJSONObject("codigo")
                                                        .optJSONObject("romaneio")
                                                        .optString("codigo")
                                        );
                                        list.add(type.cast(p));
                                    }
                                }

                                // 3) devolve o resultado
                                tcs.setResult(list);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erro fetch list", e);
                            tcs.setException(e);
                        }
                    });

               

        return tcs.getTask();
    }



    private Task<Void> sendRequest(String path, String method, JSONObject body) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String token = getAccessToken();
                RequestBody rb = RequestBody.create(body.toString(), JSON_MEDIA);
                Request.Builder builder = new Request.Builder()
                        .url(URL_API + path)
                        .addHeader("X-API-Key", token);
                Request req = method.equalsIgnoreCase("POST") ? builder.post(rb).build()
                        : builder.put(rb).build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful()) tcs.setResult(null);
                    else throw new IOException("HTTP " + resp.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro send request", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private String getAccessToken() {
        return tokenStorage.getApiKey();

    }

    private String getIdDrive() {
        try {
            String json = readFile(FILE_NAME);
            return new JSONObject(json).getJSONObject("data").optString("id");
        } catch (Exception e) {
            Log.e(TAG, "Drive ID read error", e);
            return null;
        }
    }

    private String readFile(String name) throws IOException {
        try (FileInputStream fis = context.openFileInput(name)) {
            byte[] buf = new byte[fis.available()];
            fis.read(buf);
            return new String(buf, StandardCharsets.UTF_8);
        }
    }
}
