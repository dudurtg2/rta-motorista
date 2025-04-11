package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.entities.Coletas;
import com.example.rta_app.SOLID.entities.Packet;
import com.example.rta_app.SOLID.entities.PackingList;
import com.example.rta_app.SOLID.services.TokenService;
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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PackingRepository {

    private static final String TAG = "RTAAPITEST";
    private static final String URL_API = "http://147.79.86.117:10102/";
    private static final String URL_API_GET = "http://147.79.86.117:10106/";
    private static final String FILE_NAME = "user_data.json";
    private Context context;
    private TokenService tokenService;

    public PackingRepository(Context context) {
        this.context = context;
        this.tokenService = new TokenService(context);
    }


    public Task<List<Coletas>> colectPack() {
        String accessToken = getAccessTokenFromLocalFile();
        return getPackingListToApiListNotColet(accessToken);
    }

    public Task<Void> postPacked(String codigo) {
        tokenService.validateAndRefreshToken();
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        return postCodigoList(codigo, taskCompletionSource);
    }


    public Task<List<Packet>> getListPacking() {
        String accessToken = getAccessTokenFromLocalFile();
        return getPackingListToApiList(accessToken);
    }

    private String getAccessTokenFromLocalFile() {
        tokenService.validateAndRefreshToken();
        try {
            String jsonContent = readFile(FILE_NAME);
            Log.d(TAG, "Conteúdo do arquivo JSON: " + jsonContent);

            JSONObject jsonObject = new JSONObject(jsonContent);

            if (jsonObject.has("accessToken")) {
                return jsonObject.getString("accessToken");
            } else {
                Log.e(TAG, "Campo 'accessToken' não encontrado no arquivo JSON");
                return null;
            }

        } catch (IOException e) {
            Log.e(TAG, "Erro ao ler o arquivo de token", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar o conteúdo do arquivo", e);
            return null;
        }
    }

    private Task<Void> postCodigoList(String codigo, TaskCompletionSource<Void> taskCompletionSource) {

        tokenService.validateAndRefreshToken();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("codigo", codigo);
            jsonBody.put("coletado", true);

        } catch (JSONException e) {
            taskCompletionSource.setException(e);
            return taskCompletionSource.getTask();
        }

        String accessToken = getAccessTokenFromLocalFile();
        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(URL_API + "api/devolucao/save/" + getIdDriveFromLocalFile())
                .post(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        new Thread(() -> {
            try (Response response = new OkHttpClient().newCall(request).execute()) {
                String responseBody = response.body().string();  // Obtendo o corpo da resposta como String

                if (response.isSuccessful()) {
                    Log.d(TAG, "Motorista atualizado com sucesso: " + responseBody);
                    taskCompletionSource.setResult(null);
                } else {
                    Log.e(TAG, "Erro na requisição PUT: " + responseBody);  // Aqui usamos o conteúdo da resposta
                    taskCompletionSource.setException(
                            new Exception("Erro na requisição PUT: " + responseBody));
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao fazer a requisição PUT", e);
                taskCompletionSource.setException(e);
            }
        }).start();

        return taskCompletionSource.getTask();
    }


    private String getIdDriveFromLocalFile() {

        try {
            String jsonContent = readFile(FILE_NAME);
            Log.d(TAG, "Conteúdo do arquivo JSON: " + jsonContent);

            JSONObject jsonObject = new JSONObject(jsonContent);

            JSONObject info = jsonObject.getJSONObject("data");


            if (info.has("id")) {
                return info.getString("id");
            } else {
                Log.e(TAG, "Campo 'id' não encontrado no arquivo JSON");
                return null;
            }

        } catch (IOException e) {
            Log.e(TAG, "Erro ao ler o arquivo de token", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar o conteúdo do arquivo", e);
            return null;
        }
    }

    private Task<List<Coletas>> getPackingListToApiListNotColet(String accessToken) {
        TaskCompletionSource<List<Coletas>> taskCompletionSource = new TaskCompletionSource<>();

        Request request = new Request.Builder()
                .url(URL_API_GET + "api/devolucao/findByMotoristaNotColect/" + getIdDriveFromLocalFile())
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        ;
        new Thread(() -> {
            try (Response response = new OkHttpClient().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Requisição GET bem-sucedida: " + responseBody);
                    JSONArray jsonArray = new JSONArray(responseBody);
                    List<Coletas> packingLists = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);


                        Coletas packingList = new Coletas();

                        packingList.setEntregador(jsonObject.optJSONObject("entregador").optString("nome", ""));
                        packingList.setCodigos(jsonObject.optString("codigos", ""));
                        packingList.setQtd(jsonObject.optString("quantidade", ""));


                        packingLists.add(packingList);
                        Log.d(TAG, "Aqui o resultado " + packingList);

                    }
                    taskCompletionSource.setResult(packingLists);
                } else {
                    Log.e(TAG, "Erro na requisição GET: " + response.code() + " " + response.message());
                    taskCompletionSource.setException(
                            new Exception("Erro na requisição GET: " + response.code() + " " + response.message()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao fazer a requisição GET", e);
                taskCompletionSource.setException(e);
            }
        }).start();

        return taskCompletionSource.getTask();
    }

    private Task<List<Packet>> getPackingListToApiList(String accessToken) {
        TaskCompletionSource<List<Packet>> taskCompletionSource = new TaskCompletionSource<>();

        Request request = new Request.Builder()
                .url(URL_API_GET + "api/devolucao/findByMotorista/" + getIdDriveFromLocalFile())
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        ;
        new Thread(() -> {
            try (Response response = new OkHttpClient().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Requisição GET bem-sucedida: " + responseBody);
                    JSONArray jsonArray = new JSONArray(responseBody);
                    List<Packet> packingLists = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);


                        Packet packingList = new Packet();

                        packingList.setEntregador(jsonObject.optJSONObject("entregador").optString("nome", ""));
                        packingList.setCodigo(jsonObject.optJSONObject("codigo").optString("codigo", ""));
                        packingList.setData(jsonObject.optString("dataDevolvido", "").replace("T", " ").split("\\.")[0]);
                        packingList.setRta(jsonObject.optJSONObject("codigo").optJSONObject("romaneio").optString("codigo", ""));

                        packingLists.add(packingList);
                        Log.d(TAG, "Aqui o resultado " + packingList);

                    }
                    taskCompletionSource.setResult(packingLists);
                } else {
                    Log.e(TAG, "Erro na requisição GET: " + response.code() + " " + response.message());
                    taskCompletionSource.setException(
                            new Exception("Erro na requisição GET: " + response.code() + " " + response.message()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao fazer a requisição GET", e);
                taskCompletionSource.setException(e);
            }
        }).start();

        return taskCompletionSource.getTask();
    }

    private String readFile(String fileName) throws IOException {
        try (FileInputStream fis = context.openFileInput(fileName)) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }
}
