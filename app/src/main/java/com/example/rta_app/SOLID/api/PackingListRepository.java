package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.example.rta_app.SOLID.entities.PackingList;
import com.example.rta_app.SOLID.services.ApiClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

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

import okhttp3.Request;
import okhttp3.RequestBody;

public class PackingListRepository {

    private static final String TAG = "PackingListRepo";
    private static final String FILE_NAME = "user_data.json";

    private final Context context;
    private final ApiClient apiClient;
    private final ExecutorService executor;

    public PackingListRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiClient = new ApiClient(this.context);
        this.executor = Executors.newSingleThreadExecutor();
        Log.d(TAG, "PackingListRepository initialized");
    }

    public Task<PackingList> getPackingListToDirect(String id) {
        return getPackingList(id, "alocado");
    }

    public Task<PackingList> getPackingListToRota(String id) {
        return getPackingList(id, "retirado");
    }

    public Task<PackingList> getPackingListToBase(String id) {
        return getPackingList(id, "aguardando");
    }

    public Task<List<PackingList>> getListPackingListBase() {
        return getPackingListList("retirado");
    }

    public Task<Void> movePackingListForDelivery(PackingList packingList) {
        Log.d(TAG, "movePackingListForDelivery: " + packingList.getCodigodeficha());
        return updateRomaneiosNome(packingList);
    }

    public Task<Void> updateStatusPackingList(PackingList packingList, String ocorrencia, String status) {
        Log.d(TAG, "updateStatusPackingList: ficha=" + packingList.getCodigodeficha() + ", status=" + status);

        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                if (ocorrencia != null && !ocorrencia.isEmpty()) {
                    body.put("ocorrencia", ocorrencia);
                }

                body.put("status", status);
                body.put("dataFinal", getCurrentDateTime());

                executeRequest("api/romaneios/update/codigo/" + packingList.getCodigodeficha(), "PUT", body);
                tcs.setResult(null);
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
                executeRequest("api/romaneios/imageUpload/" + uid, "POST", body);
                tcs.setResult(null);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao enviar imagem", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private Task<PackingList> getPackingList(String id, String sts) {
        Log.d(TAG, "getPackingList ID: " + id + ", filtro: " + sts);

        TaskCompletionSource<PackingList> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                Request request = apiClient.authenticatedRequest("api/romaneios/findBySearch/" + id)
                        .get()
                        .build();

                JSONObject json = new JSONObject(apiClient.executeForBody(request));
                String stsFilter = json.optString("sts");

                if (stsFilter.equals("finalizado") || stsFilter.equals("inativo")) {
                    throw new IllegalStateException("Status filter invalido: " + stsFilter);
                }
                if (sts.equals("aguardando") || sts.equals("alocado")) {
                    if (stsFilter.equals("retirado") || stsFilter.equals("recusado")) {
                        throw new IllegalStateException("Status filter invalido: " + stsFilter);
                    }
                }

                PackingList pl = parsePackingList(json, sts);
                tcs.setResult(pl);
            } catch (Exception e) {
                Log.e(TAG, "Erro GET single", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private Task<List<PackingList>> getPackingListList(String sts) {
        Log.d(TAG, "getPackingListList status: " + sts);

        TaskCompletionSource<List<PackingList>> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String driverId = getIdDriveOrThrow();

                Request request = apiClient.authenticatedRequest("api/romaneios/getMinimalDriverAll/" + driverId + "/" + sts)
                        .get()
                        .build();

                JSONArray arr = new JSONArray(apiClient.executeForBody(request));
                List<PackingList> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    list.add(parseMinimal(arr.getJSONObject(i)));
                }
                tcs.setResult(list);
            } catch (Exception e) {
                Log.e(TAG, "Erro GET list", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private void executeRequest(String path, String method, JSONObject body) throws IOException {
        RequestBody rb = RequestBody.create(body.toString(), ApiClient.JSON_MEDIA);
        Request.Builder builder = apiClient.authenticatedRequest(path);
        Request req = "PUT".equals(method) ? builder.put(rb).build() : builder.post(rb).build();
        apiClient.executeForNoBody(req);
    }

    private PackingList parsePackingList(JSONObject json, String stsFilter) throws JSONException {
        PackingList pl = new PackingList();
        pl.setFuncionario(optObjectString(json, "funcionario", "nome"));
        pl.setEntregador(optObjectString(json, "entregador", "nome"));
        pl.setTelefone(optObjectString(json, "entregador", "telefone"));
        pl.setCodigodeficha(json.optString("codigoUid"));
        pl.setHoraedia(json.optString("data"));
        pl.setQuantidade(String.valueOf(json.optInt("quantidade")));
        pl.setStatus(stsFilter);
        pl.setMotorista(json.optString("motorista"));
        pl.setDownloadlink(json.optString("linkDownload"));
        pl.setEmpresa(optObjectString(json, "empresa", "nome"));
        pl.setEndereco(optObjectString(json, "entregador", "endereco"));
        pl.setLocal(parseCidades(json.optJSONArray("cidade")));
        pl.setCodigosinseridos(parseCodigos(json.optJSONArray("codigos")));
        return pl;
    }

    private PackingList parseMinimal(JSONObject json) {
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

    private Task<Void> updateRomaneiosNome(PackingList pl) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject()
                        .put("status", "retirado")
                        .put("motorista", getIdDriveOrThrow());

                executeRequest("api/romaneios/update/codigo/" + pl.getCodigodeficha(), "PUT", body);
                tcs.setResult(null);
            } catch (Exception e) {
                Log.e(TAG, "Erro updateRomaneio", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
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
        if (cidades == null) {
            return "";
        }

        StringBuilder loc = new StringBuilder();
        for (int i = 0; i < cidades.length(); i++) {
            String cn = cidades.getJSONObject(i).optString("nome");
            if (!cn.isEmpty()) {
                if (loc.length() > 0) {
                    loc.append(", ");
                }
                loc.append(cn);
            }
        }
        return loc.toString();
    }

    private List<String> parseCodigos(JSONArray cods) throws JSONException {
        List<String> codes = new ArrayList<>();
        if (cods == null) {
            return codes;
        }

        for (int i = 0; i < cods.length(); i++) {
            JSONObject c = cods.getJSONObject(i);
            String prefix = "PACOTES".equals(c.optString("type")) ? "\u2709\uFE0F" : "\uD83D\uDCE6";
            codes.add(prefix + " - " + c.optString("codigo"));
        }
        return codes;
    }

    private static String optObjectString(JSONObject source, String objectName, String fieldName) {
        JSONObject object = source.optJSONObject(objectName);
        return object == null ? "" : object.optString(fieldName);
    }
}
