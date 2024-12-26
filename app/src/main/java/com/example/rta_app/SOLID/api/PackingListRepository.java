package com.example.rta_app.SOLID.api;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.example.rta_app.SOLID.Interfaces.IPackingListRepository;
import com.example.rta_app.SOLID.entities.PackingList;
import com.example.rta_app.SOLID.services.TokenService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import android.content.Context;

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
import java.util.concurrent.CompletableFuture;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PackingListRepository implements IPackingListRepository {

    private static final String TAG = "RTAAPITEST";
    private static final String URL_API = "https://api.avalonstudios-rta.site/";
    private static final String FILE_NAME = "user_data.json";
    private Context context;
    private TokenService tokenService;
    public PackingListRepository(Context context) {
        this.context = context;
        this.tokenService = new TokenService(context);
    }

    @Override
    public Task<Void> finishPackingList() {
        return null;
    }

    @Override
    public Task<PackingList> getPackingListToDirect(String id) {
        String accessToken = getAccessTokenFromLocalFile();
        return getPackingListToApi(id, "alocado", accessToken);
    }

    @Override
    public Task<PackingList> getPackingListToRota(String id) {
        String accessToken = getAccessTokenFromLocalFile();
        return getPackingListToApi(id, "retirado", accessToken);
    }

    @Override
    public Task<PackingList> getPackingListToBase(String id) {
        String accessToken = getAccessTokenFromLocalFile();
        return getPackingListToApi(id, "aguardando", accessToken);
    }

    @Override
    public Task<List<PackingList>> getListPackingListToDirect() {
        String accessToken = getAccessTokenFromLocalFile();
        return getPackingListToApiList("alocado", accessToken);
    }

    @Override
    public Task<List<PackingList>> getListPackingListBase() {
        String accessToken = getAccessTokenFromLocalFile();
        return getPackingListToApiList("retirado", accessToken);
    }

    @Override
    public Task<Void> movePackingListForDelivery(PackingList packingList) {
        tokenService.validateAndRefreshToken();
        try {
            updateRomaneiosNome(packingList);
            return Tasks.forResult(null);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar o usuário", e);
            return Tasks.forException(e);
        }
    }

    @Override
    public Task<Void> updateStatusPackingList(PackingList packingList, String ocorrencia, String status) {
        tokenService.validateAndRefreshToken();
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        JSONObject jsonBody = new JSONObject();
        try {
            if (!ocorrencia.isEmpty()) {
                jsonBody.put("ocorrencia", ocorrencia);
            }
            jsonBody.put("status", status);
            jsonBody.put("dataFinal", new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
            Log.e(TAG, jsonBody.toString());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao criar o corpo da requisição", e);
            taskCompletionSource.setException(e);
            return taskCompletionSource.getTask();
        }

        return putPackingList(jsonBody, packingList.getCodigodeficha(), taskCompletionSource);
    }
   @Override
    public Task<Void> updateImgLinkForFinish(Bitmap bitmap, String uid) {
       TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        return postImagenList(bitmap, uid, taskCompletionSource);
    }

    private Task<List<PackingList>> getPackingListToApiList(String sts, String accessToken) {

        TaskCompletionSource<List<PackingList>> taskCompletionSource = new TaskCompletionSource<>();

        String driverId = getIdDriveFromLocalFile();
        String url = "http://carlo4664.c44.integrator.host:10500/api/romaneios/count/driver/" + driverId + "/" + sts;
        Log.d(TAG, "URL: " + url);
        // Criando a requisição GET
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken) // Adicionando o token de autorização
                .build();

        // Executando a requisição em uma Thread separada
        new Thread(() -> {
            try (Response response = new OkHttpClient().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string(); // Pegando o corpo da resposta
                    Log.d(TAG, "Requisição GET bem-sucedida: " + responseBody);

                    // Criando um objeto JSONArray para processar os dados manualmente
                    JSONArray jsonArray = new JSONArray(responseBody);
                    List<PackingList> packingLists = new ArrayList<>();

                    // Iterando sobre cada item do array
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        // Criando e preenchendo manualmente o objeto PackingList
                        PackingList packingList = new PackingList();
                        if (jsonObject.optString("sts", "").equals(sts)) {

                            // Mapeando os campos necessários
                            packingList.setFuncionario(jsonObject.optJSONObject("funcionario").optString("nome", ""));
                            packingList.setEntregador(jsonObject.optJSONObject("entregador").optString("nome", ""));
                            packingList.setTelefone(jsonObject.optJSONObject("entregador").optString("telefone", ""));

                            packingList.setCodigodeficha(jsonObject.optString("codigoUid", ""));
                            packingList.setHoraedia(jsonObject.optString("data", ""));
                            packingList.setQuantidade(String.valueOf(jsonObject.optInt("quantidade", 0)));
                            packingList.setStatus(jsonObject.optString("sts", ""));
                            packingList.setMotorista(jsonObject.optString("motorista", ""));
                            packingList.setDownloadlink(jsonObject.optString("linkDownload", ""));
                            packingList.setEmpresa(jsonObject.optJSONObject("empresa").optString("nome", ""));
                            packingList.setEndereco(jsonObject.optJSONObject("entregador").optString("endereco", ""));

                            // Preenchendo manualmente os códigos inseridos
                            JSONArray codigosArray = jsonObject.optJSONArray("codigos");
                            List<String> codigosInseridos = new ArrayList<>();
                            if (codigosArray != null) {
                                for (int j = 0; j < codigosArray.length(); j++) {
                                    codigosInseridos.add(codigosArray.getJSONObject(j).optString("codigo", ""));
                                }
                            }


                            JSONArray locallist = jsonObject.optJSONArray("cidade");

                            String local = ""; // Inicializar uma string vazia

                            if (locallist != null) {
                                // Iterar sobre cada objeto no array de cidades
                                for (int o = 0; o < locallist.length(); o++) {
                                    JSONObject cidadeObject = locallist.optJSONObject(o);
                                    if (cidadeObject != null) {
                                        String cidadeNome = cidadeObject.optString("nome", "");
                                        if (!cidadeNome.isEmpty()) {
                                            // Adicionar o nome da cidade e uma vírgula
                                            if (!local.isEmpty()) {
                                                local += ", ";
                                            }
                                            local += cidadeNome;
                                        }
                                    }
                                }
                            }

                            packingList.setLocal(local);
                            packingList.setCodigosinseridos(codigosInseridos);

                            // Adicionando o PackingList à lista
                            packingLists.add(packingList);
                        }
                    }
                    taskCompletionSource.setResult(packingLists);
                } else {
                    Log.e(TAG, "Erro na requisição GET: " + response.code() + " " + response.message());
                    taskCompletionSource.setException(
                            new Exception("Erro na requisição GET: " + response.code() + " " + response.message()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao fazer a requisição GET", e);
                taskCompletionSource.setException(e); // Completa a Task com erro
            }
        }).start();

        return taskCompletionSource.getTask(); // Retorna a Task criada
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
    private Task<Void> putPackingList(JSONObject jsonBody, String uid, TaskCompletionSource<Void> taskCompletionSource) {
        tokenService.validateAndRefreshToken();


        String accessToken = getAccessTokenFromLocalFile();
        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(URL_API + "api/romaneios/update/codigo/" + uid)
                .put(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        new Thread(() -> {
            try (Response response = new OkHttpClient().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Motorista atualizado com sucesso: " + response.body().string());
                    taskCompletionSource.setResult(null);
                } else {
                    Log.e(TAG, "Erro na requisição PUT: " + response.code() + " " + response.message());
                    taskCompletionSource.setException(
                            new Exception("Erro na requisição PUT: " + response.code() + " " + response.message()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao fazer a requisição PUT", e);
                taskCompletionSource.setException(e);
            }
        }).start();

        return taskCompletionSource.getTask();
    }

    private Task<Void> postImagenList(Bitmap bitmap, String uid, TaskCompletionSource<Void> taskCompletionSource) {

        tokenService.validateAndRefreshToken();

        String base64Bitmap = bitmapToBase64(bitmap);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("base64Image", base64Bitmap);
        } catch (JSONException e) {
            taskCompletionSource.setException(e);
            return taskCompletionSource.getTask();
        }

        String accessToken = getAccessTokenFromLocalFile();
        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(URL_API + "api/romaneios/imageUpload/" + uid)
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



    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 75, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
    private String getIdDriveFromLocalFile() {

        try {
            String jsonContent = readFile(FILE_NAME);
            Log.d(TAG, "Conteúdo do arquivo JSON: " + jsonContent);

            JSONObject jsonObject = new JSONObject(jsonContent);

            JSONObject data = jsonObject.getJSONObject("data");
            JSONObject info = data.getJSONObject("info");

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
    private Task<PackingList> getPackingListToApi(String id, String sts, String accessToken) {
        TaskCompletionSource<PackingList> taskCompletionSource = new TaskCompletionSource<>();
        Request request = new Request.Builder()
                .url(URL_API + "api/romaneios/findBySearch/" + id)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        new Thread(() -> {
            try (Response response = new OkHttpClient().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Requisição GET bem-sucedida: " + responseBody);

                    JSONObject jsonObject = new JSONObject(responseBody);

                    PackingList packingList = new PackingList();
                    if (jsonObject.optString("sts", "").equals(sts)) {

                        packingList.setFuncionario(jsonObject.optJSONObject("funcionario").optString("nome", ""));
                        packingList.setEntregador(jsonObject.optJSONObject("entregador").optString("nome", ""));
                        packingList.setTelefone(jsonObject.optJSONObject("entregador").optString("telefone", ""));
                        packingList.setCodigodeficha(jsonObject.optString("codigoUid", ""));
                        packingList.setHoraedia(jsonObject.optString("data", ""));
                        packingList.setQuantidade(String.valueOf(jsonObject.optInt("quantidade", 0)));
                        packingList.setStatus(jsonObject.optString("sts", ""));
                        packingList.setMotorista(jsonObject.optString("motorista", ""));
                        packingList.setDownloadlink(jsonObject.optString("linkDownload", ""));
                        packingList.setEmpresa(jsonObject.optJSONObject("empresa").optString("nome", ""));
                        packingList.setEndereco(jsonObject.optJSONObject("entregador").optString("endereco", ""));

                        JSONArray locallist = jsonObject.optJSONArray("cidade");

                        String local = ""; // Inicializar uma string vazia

                        if (locallist != null) {
                            // Iterar sobre cada objeto no array de cidades
                            for (int o = 0; o < locallist.length(); o++) {
                                JSONObject cidadeObject = locallist.optJSONObject(o);
                                if (cidadeObject != null) {
                                    String cidadeNome = cidadeObject.optString("nome", "");
                                    if (!cidadeNome.isEmpty()) {
                                        // Adicionar o nome da cidade e uma vírgula
                                        if (!local.isEmpty()) {
                                            local += ", ";
                                        }
                                        local += cidadeNome;
                                    }
                                }
                            }
                        }

                        JSONArray codigosArray = jsonObject.optJSONArray("codigos");
                        List<String> codigosInseridos = new ArrayList<>();
                        if (codigosArray != null) {
                            for (int i = 0; i < codigosArray.length(); i++) {
                                codigosInseridos.add(codigosArray.getJSONObject(i).optString("codigo", ""));
                            }
                        }

                        packingList.setLocal(local);
                        packingList.setCodigosinseridos(codigosInseridos);

                        taskCompletionSource.setResult(packingList);
                    } else {
                        taskCompletionSource.setException(
                                new Exception("Erro na requisição GET: " + response.code() + " " + response.message()));
                    }
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
    private void updateRomaneiosNome(PackingList packingList) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("status", "retirado");
            jsonBody.put("motorista", getIdDriveFromLocalFile());
            Log.d(TAG, jsonBody.toString());
        } catch (Exception e) {
            Log.d(TAG, "Erro ao criar o corpo da requisição", e);
        }

        String accessToken = getAccessTokenFromLocalFile();

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(URL_API + "api/romaneios/update/codigo/" + packingList.getCodigodeficha())
                .put(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        new Thread(() -> {
            try (Response response = new OkHttpClient().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Motorista atualizado com sucesso: " + response.body().string());
                } else {
                    Log.d(TAG, "Erro na requisição PUT: " + response.code() + " " + response.message());
                }
            } catch (Exception e) {
                Log.d(TAG, "Erro ao fazer a requisição PUT", e);
            }
        }).start();
    }
    private String readFile(String fileName) throws IOException {
        try (FileInputStream fis = context.openFileInput(fileName)) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }
}
