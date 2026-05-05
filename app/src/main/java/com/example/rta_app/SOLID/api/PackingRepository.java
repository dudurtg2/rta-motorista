package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.entities.Coletas;
import com.example.rta_app.SOLID.entities.Packet;
import com.example.rta_app.SOLID.services.ApiClient;
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

import okhttp3.Request;
import okhttp3.RequestBody;

public class PackingRepository {
    private static final String TAG = "PackingRepo";
    private static final String FILE_NAME = "user_data.json";

    private final Context context;
    private final ApiClient apiClient;
    private final ExecutorService executor;

    public PackingRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiClient = new ApiClient(this.context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Task<List<Coletas>> colectPack() {
        return fetchList("api/devolucao/findByMotoristaNotColect/", Coletas.class);
    }

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
        return sendRequestForDriver("api/devolucao/save/", "POST", body);
    }

    public Task<List<Packet>> getListPacking() {
        return fetchList("api/devolucao/findByMotorista/", Packet.class);
    }

    private <T> Task<List<T>> fetchList(String path, Class<T> type) {
        TaskCompletionSource<List<T>> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String driverId = getIdDriveOrThrow();
                Request req = apiClient.authenticatedRequest(path + driverId)
                        .get()
                        .build();

                JSONArray arr = new JSONArray(apiClient.executeForBody(req));
                List<T> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    if (type == Coletas.class) {
                        list.add(type.cast(parseColeta(obj)));
                    } else {
                        list.add(type.cast(parsePacket(obj)));
                    }
                }

                tcs.setResult(list);
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
                executeRequest(path, method, body);
                tcs.setResult(null);
            } catch (Exception e) {
                Log.e(TAG, "Erro send request", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private Task<Void> sendRequestForDriver(String pathPrefix, String method, JSONObject body) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                executeRequest(pathPrefix + getIdDriveOrThrow(), method, body);
                tcs.setResult(null);
            } catch (Exception e) {
                Log.e(TAG, "Erro send request", e);
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

    private Coletas parseColeta(JSONObject obj) {
        Coletas c = new Coletas();
        c.setEntregador(optObjectString(obj, "entregador", "nome"));
        c.setCodigos(obj.optString("codigos"));
        c.setQtd(obj.optString("quantidade"));
        return c;
    }

    private Packet parsePacket(JSONObject obj) {
        JSONObject codigo = obj.optJSONObject("codigo");
        JSONObject romaneio = codigo == null ? null : codigo.optJSONObject("romaneio");

        Packet p = new Packet();
        p.setEntregador(optObjectString(obj, "entregador", "nome"));
        p.setCodigo(codigo == null ? "" : codigo.optString("codigo"));
        p.setData(formatApiDate(obj.optString("dataDevolvido")));
        p.setRta(romaneio == null ? "" : romaneio.optString("codigo"));
        return p;
    }

    private String getIdDriveOrThrow() throws IOException, JSONException {
        String json = readFile(FILE_NAME);
        String driverId = new JSONObject(json).getJSONObject("data").optString("id");
        if (driverId == null || driverId.trim().isEmpty()) {
            throw new IllegalStateException("Driver ID invalido");
        }
        return driverId;
    }

    private String readFile(String name) throws IOException {
        try (FileInputStream fis = context.openFileInput(name)) {
            byte[] buf = new byte[fis.available()];
            fis.read(buf);
            return new String(buf, StandardCharsets.UTF_8);
        }
    }

    private static String optObjectString(JSONObject source, String objectName, String fieldName) {
        JSONObject object = source.optJSONObject(objectName);
        return object == null ? "" : object.optString(fieldName);
    }

    private static String formatApiDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) {
            return "";
        }
        String[] parts = rawDate.replace("T", " ").split("\\.");
        return parts.length > 0 ? parts[0] : rawDate;
    }
}
