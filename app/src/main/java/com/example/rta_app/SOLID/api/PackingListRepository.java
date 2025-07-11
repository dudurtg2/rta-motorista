package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.example.rta_app.SOLID.entities.PackingList;
import com.example.rta_app.SOLID.services.TokenService;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PackingListRepository {
    private static final String TAG = "PackingListRepo";
    private static final String URL_API = "https://android.lc-transportes.com/";
    private static final String URL_API_GET = "https://android.lc-transportes.com/";
    private static final String FILE_NAME = "user_data.json";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private final TokenService tokenService;
    private final OkHttpClient httpClient;
    private final TokenStorage tokenStorage;
    private final ExecutorService executor;

    public PackingListRepository(Context context) {
        this.context = context.getApplicationContext();
        this.tokenService = new TokenService(this.context);
        this.tokenStorage = new TokenStorage(this.context);
        this.httpClient = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();
    }

   
    public Task<Void> finishPackingList() {
        tokenService.validateAndRefreshToken();
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            // TODO: implementar lógica de fechamento de packing list
            tcs.setResult(null);
        });
        return tcs.getTask();
    }

   
    public Task<PackingList> getPackingListToDirect(String id) {
        String token = getAccessToken();
        return getPackingList(id, "alocado", token);
    }

   
    public Task<PackingList> getPackingListToRota(String id) {
        String token = getAccessToken();
        return getPackingList(id, "retirado", token);
    }

   
    public Task<PackingList> getPackingListToBase(String id) {
        String token = getAccessToken();
        return getPackingList(id, token);
    }

   
    public Task<List<PackingList>> getListPackingListToDirect() {
        String token = getAccessToken();
        return getPackingListList("alocado", token);
    }

   
    public Task<List<PackingList>> getListPackingListBase() {
        String token = getAccessToken();
        return getPackingListList("retirado", token);
    }

   
    public Task<Void> movePackingListForDelivery(PackingList packingList) {
        tokenService.validateAndRefreshToken();
        try {
            updateRomaneiosNome(packingList);
            return Tasks.forResult(null);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao mover packing list", e);
            return Tasks.forException(e);
        }
    }

   
    public Task<Void> updateStatusPackingList(PackingList packingList, String ocorrencia, String status) {
        tokenService.validateAndRefreshToken();
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                if (!ocorrencia.isEmpty()) body.put("ocorrencia", ocorrencia);
                body.put("status", status);
                String formattedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .format(new Date());
                body.put("dataFinal", formattedDate);
                executeRequest("api/romaneios/update/codigo/" + packingList.getCodigodeficha(), "PUT", body, tcs);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao atualizar status", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

   
    public Task<Void> updateImgLinkForFinish(Bitmap bitmap, String uid) {
        tokenService.validateAndRefreshToken();
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String base64 = bitmapToBase64(bitmap);
                JSONObject body = new JSONObject();
                body.put("base64Image", base64);
                executeRequest("api/romaneios/imageUpload/" + uid, "POST", body, tcs);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao enviar imagem", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    // --- Helpers ---

    private Task<PackingList> getPackingList(String id, String sts, String token) {
        TaskCompletionSource<PackingList> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(URL_API_GET + "api/romaneios/findBySearch/" + id)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();
                try (Response resp = httpClient.newCall(request).execute()) {
                    if (!resp.isSuccessful()) throw new IOException(resp.message());
                    JSONObject json = new JSONObject(resp.body().string());
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

    private Task<PackingList> getPackingList(String id, String token) {
        return getPackingList(id, null, token);
    }

    private Task<List<PackingList>> getPackingListList(String sts, String token) {
        TaskCompletionSource<List<PackingList>> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String driverId = getIdDrive();
                Request request = new Request.Builder()
                        .url(URL_API_GET + "api/romaneios/getMinimalDriverAll/" + driverId + "/" + sts)
                        .addHeader("Authorization", "Bearer " + token)
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
                Request.Builder rbuilder = new Request.Builder()
                        .url(URL_API + path)
                        .addHeader("Authorization", "Bearer " + getAccessToken());
                Request req = method.equals("PUT")
                        ? rbuilder.put(rb).build()
                        : rbuilder.post(rb).build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful()) tcs.setResult(null);
                    else throw new IOException(resp.message());
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro operação", e);
                tcs.setException(e);
            }
        });
    }

    private PackingList parsePackingList(JSONObject json, String stsFilter) throws JSONException {
        String sts = json.optString("sts");
        if (stsFilter != null && !sts.equals(stsFilter)) {
            throw new JSONException("Status inválido: " + sts);
        }
        PackingList pl = new PackingList();
        pl.setFuncionario(json.optJSONObject("funcionario").optString("nome"));
        pl.setEntregador(json.optJSONObject("entregador").optString("nome"));
        pl.setTelefone(json.optJSONObject("entregador").optString("telefone"));
        pl.setCodigodeficha(json.optString("codigoUid"));
        pl.setHoraedia(json.optString("data"));
        pl.setQuantidade(String.valueOf(json.optInt("quantidade")));
        pl.setStatus(sts);
        pl.setMotorista(json.optString("motorista"));
        pl.setDownloadlink(json.optString("linkDownload"));
        pl.setEmpresa(json.optJSONObject("empresa").optString("nome"));
        pl.setEndereco(json.optJSONObject("entregador").optString("endereco"));

        // Cidades
        JSONArray cidades = json.optJSONArray("cidade");
        StringBuilder loc = new StringBuilder();
        if (cidades != null) {
            for (int i = 0; i < cidades.length(); i++) {
                String cn = cidades.getJSONObject(i).optString("nome");
                if (!cn.isEmpty()) {
                    if (loc.length() > 0) loc.append(", ");
                    loc.append(cn);
                }
            }
        }
        pl.setLocal(loc.toString());

        // Códigos
        JSONArray cods = json.optJSONArray("codigos");
        List<String> codes = new ArrayList<>();
        if (cods != null) {
            for (int i = 0; i < cods.length(); i++) {
                JSONObject c = cods.getJSONObject(i);
                String prefix = c.optString("type").equals("PACOTES") ? "✉️" : "📦";
                codes.add(prefix + " - " + c.optString("codigo"));
            }
        }
        pl.setCodigosinseridos(codes);
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
                JSONObject body = new JSONObject();
                body.put("status", "retirado");
                body.put("motorista", getIdDrive());
                executeRequest("api/romaneios/update/codigo/" + pl.getCodigodeficha(), "PUT", body, new TaskCompletionSource<>());
            } catch (Exception e) {
                Log.e(TAG, "Erro updateRomaneio", e);
            }
        });
    }

    // --- File I/O and Token ---

    private String getAccessToken() {
        tokenService.validateAndRefreshToken();
        return tokenStorage.getAccessToken();
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

    private String bitmapToBase64(Bitmap bmp) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 75, os);
        return Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP);
    }
}
