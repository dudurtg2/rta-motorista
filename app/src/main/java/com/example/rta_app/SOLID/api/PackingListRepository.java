package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.example.rta_app.SOLID.entities.PackingList;
import com.example.rta_app.SOLID.services.TokenStorage;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PackingListRepository {

    private static final String TAG = "PackingListRepo";
    private static final String URL_API = "https://android.lc-transportes.com/";
    private static final String FILE_NAME = "user_data.json";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private final OkHttpClient httpClient;
    private final TokenStorage tokenStorage;
    private final ExecutorService executor;

    public PackingListRepository(Context context) {
        this.context = context.getApplicationContext();
        this.tokenStorage = new TokenStorage(this.context);
        this.httpClient = new OkHttpClient.Builder()
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();
        this.executor = Executors.newSingleThreadExecutor();
        Log.d(TAG, "PackingListRepository initialized");
    }

    // ------------------ MÉTODOS PÚBLICOS ------------------ //

    public Task<PackingList> getPackingListToDirect(String id) {
        return getPackingList(id, "alocado", getAccessToken());
    }

    public Task<PackingList> getPackingListToRota(String id) {
        return getPackingList(id, "retirado", getAccessToken());
    }

    public Task<PackingList> getPackingListToBase(String id) {
        return getPackingList(id, "aguardando", getAccessToken());
    }

    public Task<List<PackingList>> getListPackingListBase() {
        return getPackingListList("retirado", getAccessToken());
    }

    public Task<Void> movePackingListForDelivery(PackingList packingList) {
        Log.d(TAG, "movePackingListForDelivery: " + packingList.getCodigodeficha());
        try {
            updateRomaneiosNome(packingList);
            return Tasks.forResult(null);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao mover packing list", e);
            return Tasks.forException(e);
        }
    }

    public Task<Void> updateStatusPackingList(PackingList packingList, String ocorrencia, String status) {
        Log.d(TAG, "updateStatusPackingList: ficha=" + packingList.getCodigodeficha() + ", status=" + status);

        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                if (!ocorrencia.isEmpty()) body.put("ocorrencia", ocorrencia);

                body.put("status", status);
                body.put("dataFinal", getCurrentDateTime());

                executeRequest("api/romaneios/update/codigo/" + packingList.getCodigodeficha(), "PUT", body, tcs);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao atualizar status", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    public Task<Void> updateImgLinkForFinish(Bitmap bitmap, String uid) {
        Log.d(TAG, "updateImgLinkForFinish UID: " + uid);

        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String base64 = bitmapToBase64(bitmap);
                JSONObject body = new JSONObject().put("base64Image", base64);
                executeRequest("api/romaneios/imageUpload/" + uid, "POST", body, tcs);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao enviar imagem", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    // ------------------ MÉTODOS PRIVADOS ------------------ //

    private Task<PackingList> getPackingList(String id, String sts, String token) {
        Log.d(TAG, "getPackingList ID: " + id + ", filtro: " + sts);

        TaskCompletionSource<PackingList> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(URL_API + "api/romaneios/findBySearch/" + id)
                        .addHeader("X-API-Key", token)
                        .get()
                        .build();

                try (Response resp = httpClient.newCall(request).execute()) {
                    if (!resp.isSuccessful()) throw new IOException(resp.message());
                    JSONObject json = new JSONObject(resp.body().string());


                    String stsFilter = json.optString("sts");


                    if (stsFilter.equals("finalizado") || stsFilter.equals("inativo")) {
                        throw new IllegalStateException("Status filter inválido: " + stsFilter);
                    }
                    if (sts.equals("aguardando") || sts.equals("alocado")) {
                        if (stsFilter.equals("retirado") || stsFilter.equals("recusado") ) throw new IllegalStateException("Status filter inválido: " + stsFilter);
                    }

                    PackingList pl = parsePackingList(json, sts);
                    tcs.setResult(pl);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro GET single", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private Task<List<PackingList>> getPackingListList(String sts, String token) {
        Log.d(TAG, "getPackingListList status: " + sts);

        TaskCompletionSource<List<PackingList>> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String driverId = getIdDrive();

                Request request = new Request.Builder()
                        .url(URL_API + "api/romaneios/getMinimalDriverAll/" + driverId + "/" + sts)
                        .addHeader("X-API-Key", token)
                        .get()
                        .build();

                try (Response resp = httpClient.newCall(request).execute()) {
                    if (!resp.isSuccessful()) throw new IOException(resp.message());

                    JSONArray arr = new JSONArray(resp.body().string());
                    List<PackingList> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        list.add(parseMinimal(arr.getJSONObject(i)));
                    }
                    tcs.setResult(list);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro GET list", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private void executeRequest(String path, String method, JSONObject body, TaskCompletionSource<Void> tcs) {
        executor.execute(() -> {
            try {
                RequestBody rb = RequestBody.create(body.toString(), JSON_MEDIA);

                Request.Builder builder = new Request.Builder()
                        .url(URL_API + path)
                        .addHeader("X-API-Key", getAccessToken());

                Request req = "PUT".equals(method) ? builder.put(rb).build() : builder.post(rb).build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful()) {
                        tcs.setResult(null);
                    } else {
                        throw new IOException("Erro HTTP: " + resp.code() + " - " + resp.message());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro operação " + method + " " + path, e);
                tcs.setException(e);
            }
        });
    }

    private PackingList parsePackingList(JSONObject json, String stsFilter) throws JSONException {

        PackingList pl = new PackingList();
        pl.setFuncionario(json.optJSONObject("funcionario").optString("nome"));
        pl.setEntregador(json.optJSONObject("entregador").optString("nome"));
        pl.setTelefone(json.optJSONObject("entregador").optString("telefone"));
        pl.setCodigodeficha(json.optString("codigoUid"));
        pl.setHoraedia(json.optString("data"));
        pl.setQuantidade(String.valueOf(json.optInt("quantidade")));
        pl.setStatus(stsFilter);
        pl.setMotorista(json.optString("motorista"));
        pl.setDownloadlink(json.optString("linkDownload"));
        pl.setEmpresa(json.optJSONObject("empresa").optString("nome"));
        pl.setEndereco(json.optJSONObject("entregador").optString("endereco"));

        pl.setLocal(parseCidades(json.optJSONArray("cidade")));
        pl.setCodigosinseridos(parseCodigos(json.optJSONArray("codigos")));

        return pl;
    }

    private PackingList parseMinimal(JSONObject json) throws JSONException {
        PackingList pl = new PackingList();
        pl.setCodigodeficha(json.optString("codigoUid"));
        pl.setMotorista(json.optString("motorista"));
        pl.setEntregador(json.optString("entregador"));
        pl.setStatus(json.optString("status"));
        pl.setHoraedia(json.optString("data"));
        pl.setEmpresa(json.optString("empresa"));
        pl.setLocal(json.optString("cidade"));
        return pl;
    }

    private void updateRomaneiosNome(PackingList pl) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject()
                        .put("status", "retirado")
                        .put("motorista", getIdDrive());

                executeRequest("api/romaneios/update/codigo/" + pl.getCodigodeficha(), "PUT", body, new TaskCompletionSource<>());
            } catch (Exception e) {
                Log.e(TAG, "Erro updateRomaneio", e);
            }
        });
    }

    // ------------------ UTILS ------------------ //

    private String getAccessToken() {
        String token = tokenStorage.getApiKey();
        Log.d(TAG, "Access token: " + (token != null ? "[OK]" : "[NULL]"));
        return token;
    }

    private String getIdDrive() {
        try {
            String json = readFile(FILE_NAME);
            return new JSONObject(json).getJSONObject("data").optString("id");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao ler Driver ID", e);
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

    private String bitmapToBase64(Bitmap bmp) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 75, os);
        return Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP);
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }

    private String parseCidades(JSONArray cidades) throws JSONException {
        if (cidades == null) return "";
        StringBuilder loc = new StringBuilder();
        for (int i = 0; i < cidades.length(); i++) {
            String cn = cidades.getJSONObject(i).optString("nome");
            if (!cn.isEmpty()) {
                if (loc.length() > 0) loc.append(", ");
                loc.append(cn);
            }
        }
        return loc.toString();
    }

    private List<String> parseCodigos(JSONArray cods) throws JSONException {
        List<String> codes = new ArrayList<>();
        if (cods == null) return codes;

        for (int i = 0; i < cods.length(); i++) {
            JSONObject c = cods.getJSONObject(i);
            String prefix = "PACOTES".equals(c.optString("type")) ? "✉️" : "📦";
            codes.add(prefix + " - " + c.optString("codigo"));
        }
        return codes;
    }
}
