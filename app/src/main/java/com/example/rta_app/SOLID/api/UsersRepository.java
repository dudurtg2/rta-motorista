package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.Interfaces.IUsersRepository;
import com.example.rta_app.SOLID.entities.PackingList;
import com.example.rta_app.SOLID.entities.Users;
import com.example.rta_app.SOLID.services.TokenService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UsersRepository implements IUsersRepository {

    private static final String TAG = "API";
    private static final String URL_API = "http://carlo4664.c44.integrator.host:10500/";
    private static final String FILE_NAME = "user_data.json";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private TokenService tokenService;
    private final OkHttpClient client = new OkHttpClient();
    private final Context context;

    public UsersRepository(Context context) {
        this.context = context;
        this.tokenService = new TokenService(context);
    }

    @Override
    public Task<Users> getUser() {
        try {
            String jsonContent = readFile(FILE_NAME);

            JSONObject jsonObject = new JSONObject(jsonContent);

            JSONObject data = jsonObject.getJSONObject("data");
            JSONObject info = data.getJSONObject("info");

            String id = info.getString("id");
            String nome = info.getString("nome");

            return Tasks.forResult(new Users(nome, id));
        } catch (IOException e) {
            Log.e(TAG, "Erro ao ler o arquivo de usuário", e);
            return Tasks.forException(e);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar os dados do usuário", e);
            return Tasks.forException(e);
        }
    }

    @Override
    public Task<Void> saveUser(Users user) {
        tokenService.validateAndRefreshToken();
        try {
            updateUserNameInLocalFile(user.getName());
            updateMotoristaNome(user.getUid(), user.getName());

            return Tasks.forResult(null);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar o usuário", e);
            return Tasks.forException(e);
        }
    }
    @Override
    public Task<Void> loginUser(String nome, String senha) {
        String jsonBody = String.format("{\"login\": \"%s\", \"senha\": \"%s\"}", nome, senha);
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(URL_API + "auth/login")
                .post(body)
                .build();

        CompletableFuture<Void> future = new CompletableFuture<>();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Resposta do servidor: " + responseBody);
                    JSONObject jsonObject = new JSONObject(responseBody);

                    if (!jsonObject.optJSONObject("data").optString("cargo").equals("MOTORISTA")) throw new RuntimeException("Usuário não é motorista");

                    saveApiResponse(responseBody);

                    future.complete(null);
                } else {
                    Log.e(TAG, "Erro na requisição: " + response.code() + " - " + response.message());
                    future.completeExceptionally(new IOException("Erro na requisição: " + response.code()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao realizar o login", e);
                future.completeExceptionally(e);
            }
        }).start();

        return Tasks.call(future::join);
    }

    private void updateUserNameInLocalFile(String newName) {
        try {

            String jsonContent = readFile(FILE_NAME);
            JSONObject jsonObject = new JSONObject(jsonContent);

            if (jsonObject.has("data")) {
                JSONObject data = jsonObject.getJSONObject("data");
                if (data.has("info")) {
                    JSONObject info = data.getJSONObject("info");
                    info.put("nome", newName);
                }
            }
            writeFile(FILE_NAME, jsonObject.toString());
            Log.d(TAG, "Campo 'nome' atualizado no arquivo JSON com sucesso.");
        } catch (IOException e) {
            Log.e(TAG, "Erro ao ler o arquivo JSON para atualizar o nome", e);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao atualizar o campo 'nome' no JSON", e);
        }
    }
    private void updateMotoristaNome(String id, String nome) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("nome", nome);
            Log.e(TAG, jsonBody.toString());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao criar o corpo da requisição", e);
        }

        String accessToken = getAccessTokenFromLocalFile();

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(URL_API + "api/motoristas/update/" + id)
                .put(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        new Thread(() -> {
            try (Response response = new OkHttpClient().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Motorista atualizado com sucesso: " + response.body().string());
                } else {
                    Log.e(TAG, "Erro na requisição PUT: " + response.code() + " " + response.message());
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao fazer a requisição PUT", e);
            }
        }).start();
    }
    private String getAccessTokenFromLocalFile() {
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
    private void saveApiResponse(String responseBody) {
        try {

            writeFile(FILE_NAME, responseBody);
            Log.d(TAG, "Resposta da API salva com sucesso.");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar a resposta da API", e);
        }
    }
    private String readFile(String fileName) throws IOException {
        try (FileInputStream fis = context.openFileInput(fileName)) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }
    private void writeFile(String fileName, String content) throws IOException {
        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}

