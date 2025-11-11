package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.services.TokenStorage;
import com.example.rta_app.SOLID.entities.Users;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UsersRepository {
    private static final String TAG = "UsersRepo";
    private static final String URL_API = "https://android.lc-transportes.com/";
    private static final String FILE_NAME = "user_data.json";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final TokenStorage storage;
    private final Context context;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;

    public UsersRepository(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();
        this.storage = new TokenStorage(this.context);

    }

    /**
     * Busca apenas o objeto Users dentro de user_data.json (campo "data").
     */
    public Task<Users> getUser() {
        TaskCompletionSource<Users> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String json = readFile(FILE_NAME);
                JSONObject data = new JSONObject(json).getJSONObject("data");

                String id = data.optString("id");
                String nome = data.optString("nome");
                String telefone = data.optString("telefone");
                Boolean frete = data.optBoolean("bateponto");

                JSONArray bases = data.optJSONArray("bases");
                String baseNome = "";
                int baseId = -1;
                if (bases != null && bases.length() > 0) {
                    JSONObject b = bases.getJSONObject(0);
                    baseNome = b.optString("nome");
                    baseId = b.optInt("id");
                }

                tcs.setResult(new Users(nome, id, telefone, baseNome, baseId, frete));
            } catch (Exception e) {
                Log.e(TAG, "Erro ao obter usuário", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    /**
     * Grava localmente só o nó "data" do usuário em user_data.json.
     */
    public Task<Void> saveUser(Users user) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                // 1) Atualiza no servidor
                JSONObject body = new JSONObject()
                        .put("nome", user.getName())
                        .put("telefone", user.getTelefone().replaceAll("\\D+", ""));
                executeRequest(
                        "api/motoristas/updateSite/" + user.getUid(),
                        "PUT",
                        body,
                        tcs
                );

                // 2) Atualiza o cache local (campo data)
                String raw = readFile(FILE_NAME);
                JSONObject root = new JSONObject(raw);
                JSONObject data = root.getJSONObject("data");
                data.put("nome", user.getName());
                data.put("telefone", user.getTelefone());
                writeFile(FILE_NAME, root.toString());

            } catch (Exception e) {
                Log.e(TAG, "Erro ao salvar usuário", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    /**
     * Faz login, salva os tokens no EncryptedSharedPreferences e grava
     * em user_data.json só o bloco "data".
     */
    public Task<Void> loginUser(String nome, String senha) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                // 1) chama API de login
                JSONObject loginBody = new JSONObject()
                        .put("login", nome)
                        .put("senha", senha);
                RequestBody body = RequestBody.create(loginBody.toString(), JSON_MEDIA);

                Request req = new Request.Builder()
                        .url(URL_API + "auth/V12/login")
                        .post(body)
                        .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        throw new IOException("Login failed: " + resp.code());
                    }


                    JSONObject json = new JSONObject(resp.body().string());

                    JSONObject data = json.optJSONObject("data");
                    String role = data.optString("role");
                    if (!"MOTORISTA".equals(role)) {
                        throw new RuntimeException("Usuário não é motorista");
                    }

                    // 2) salva tokens encriptados
                    String apiKey = json.getString("apiKey");
                    storage.saveApiKey(apiKey);

                    // 3) grava só o data em user_data.json
                    JSONObject root = new JSONObject().put("data", data);
                    writeFile(FILE_NAME, root.toString());

                    tcs.setResult(null);
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro ao realizar login", e);
                storage.clear();  // limpa tokens caso falhe
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    // --- executa PUT ou POST protegidos por Bearer token ---
    private void executeRequest(
            String path,
            String method,
            JSONObject body,
            TaskCompletionSource<Void> tcs
    ) {
        executor.execute(() -> {
            try {
                // garante token válido

                String access = storage.getApiKey();
                RequestBody rb = RequestBody.create(body.toString(), JSON_MEDIA);
                Request.Builder b = new Request.Builder()
                        .url(URL_API + path)
                        .addHeader("X-API-Key", access);

                Request req = "PUT".equals(method) ? b.put(rb).build() : b.post(rb).build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful()) {
                        tcs.setResult(null);
                    } else {
                        throw new IOException("API error: " + resp.code());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro em executeRequest", e);
                tcs.setException(e);
            }
        });
    }

    // --- Helpers de I/O do bloco data em user_data.json ---
    private String readFile(String name) throws IOException {
        try (FileInputStream fis = context.openFileInput(name)) {
            byte[] buf = new byte[fis.available()];
            fis.read(buf);
            return new String(buf, StandardCharsets.UTF_8);
        }
    }

    private void writeFile(String name, String content) throws IOException {
        try (FileOutputStream fos = context.openFileOutput(name, Context.MODE_PRIVATE)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
