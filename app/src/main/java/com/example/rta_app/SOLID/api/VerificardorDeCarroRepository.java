package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.entities.VerificadoresDoCarro;
import com.example.rta_app.SOLID.services.ApiClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Request;
import okhttp3.RequestBody;

public class VerificardorDeCarroRepository {

    private static final String TAG = "VerificardorDeCarroRepositoryaa";
    private static final String USER_FILE = "user_data.json";

    private final Context context;
    private final ApiClient apiClient;
    private final ExecutorService executor;

    public VerificardorDeCarroRepository(Context context) {
        Log.d(TAG, "Constructor: initializing VerificardorDeCarroRepository");
        this.context = context.getApplicationContext();
        this.apiClient = new ApiClient(this.context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Task<Long> update(VerificadoresDoCarro verificadoresDoCarro, Long id) {
        Log.d(TAG, "update(): verificadoresDoCarro=" + verificadoresDoCarro);
        return sendReturningId("api/verificadoresDoCarros/finalizar/" + id, "PUT", verificadoresDoCarro, false);
    }

    public Task<Long> updateF(VerificadoresDoCarro verificadoresDoCarro, Long id) {
        Log.d(TAG, "updateF(): verificadoresDoCarro=" + verificadoresDoCarro);
        return sendReturningId("api/verificadoresDoCarros/finalizar/" + id, "PUT", verificadoresDoCarro, true);
    }

    public Task<Long> save(VerificadoresDoCarro verificadoresDoCarro) {
        Log.d(TAG, "save(): verificadoresDoCarro=" + verificadoresDoCarro);
        return sendReturningId("api/verificadoresDoCarros/save", "POST", verificadoresDoCarro, true);
    }

    private JSONObject buildJson(VerificadoresDoCarro verificadoresDoCarro, boolean includeCarro)
            throws IOException, JSONException {

        String driveId = getUserIdOrThrow();
        JSONObject json = new JSONObject();

        json.put("status", verificadoresDoCarro.getStatus());
        json.put("verificadorInicial", verificadoresDoCarro.getVerificadorInicial());
        json.put("verificadorFinal", verificadoresDoCarro.getVerificadorFinal());
        json.put("dataInicial", verificadoresDoCarro.getDataInicial());
        json.put("dataFinal", verificadoresDoCarro.getDataFinal());
        json.put("finalizado", verificadoresDoCarro.getFinalizado());
        json.put("latariaEsquerdaInicio", verificadoresDoCarro.getLatariaEsquerdaInicio());
        json.put("latariaEsquerdaFinal", verificadoresDoCarro.getLatariaEsquerdaFinal());
        json.put("latariaDireitaInicio", verificadoresDoCarro.getLatariaDireitaInicio());
        json.put("latariaDireitaFinal", verificadoresDoCarro.getLatariaDireitaFinal());
        json.put("painelInicio", verificadoresDoCarro.getPainelInicio());
        json.put("painelFinal", verificadoresDoCarro.getPainelFinal());
        json.put("frenteInicial", verificadoresDoCarro.getFrenteInicial());
        json.put("frenteFinal", verificadoresDoCarro.getFrenteFinal());
        json.put("atrasInicio", verificadoresDoCarro.getAtrasInicio());
        json.put("atrasFinal", verificadoresDoCarro.getAtrasFinal());

        if (includeCarro) {
            JSONObject carroJson = new JSONObject();
            carroJson.put("id", verificadoresDoCarro.getCarro());
            json.put("carro", carroJson);
        }

        JSONObject motoristaJson = new JSONObject();
        motoristaJson.put("id", driveId);
        json.put("motorista", motoristaJson);

        json.put("observacoesAdicionaisInicio", verificadoresDoCarro.getObservacoesAdicionaisInicio());
        json.put("observacoesAdicionaisFinal", verificadoresDoCarro.getObservacoesAdicionaisFinal());
        return json;
    }

    private Task<Long> sendReturningId(
            String path,
            String method,
            VerificadoresDoCarro verificadoresDoCarro,
            boolean includeCarro
    ) {
        TaskCompletionSource<Long> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                Log.d(TAG, "sendReturningId(): path=" + path);
                JSONObject json = buildJson(verificadoresDoCarro, includeCarro);
                RequestBody body = RequestBody.create(json.toString(), ApiClient.JSON_MEDIA);
                Request.Builder builder = apiClient.authenticatedRequest(path);
                Request request = "PUT".equals(method) ? builder.put(body).build() : builder.post(body).build();

                String responseBody = apiClient.executeForBody(request);
                Log.d(TAG, "sendReturningId(): body=" + responseBody);

                JSONObject responseJson = new JSONObject(responseBody);
                if (!responseJson.has("id")) {
                    throw new JSONException("Campo 'id' nao encontrado no retorno: " + responseBody);
                }

                long id = responseJson.getLong("id");
                Log.d(TAG, "sendReturningId(): parsed id=" + id);
                tcs.setResult(id);
            } catch (Exception e) {
                Log.e(TAG, "sendReturningId(): request error", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    private String getUserIdOrThrow() throws IOException, JSONException {
        Log.d(TAG, "getUserId(): reading from " + USER_FILE);
        String json = readFile(USER_FILE);
        JSONObject data = new JSONObject(json).getJSONObject("data");
        String id = data.optString("id", null);
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalStateException("Token or user ID missing");
        }
        Log.d(TAG, "getUserId(): id=" + id);
        return id;
    }

    private String readFile(String name) throws IOException {
        Log.d(TAG, "readFile(): name=" + name);
        try (FileInputStream fis = context.openFileInput(name)) {
            byte[] buf = new byte[fis.available()];
            fis.read(buf);
            String result = new String(buf, StandardCharsets.UTF_8);
            Log.d(TAG, "readFile(): read length=" + result.length());
            return result;
        }
    }
}
