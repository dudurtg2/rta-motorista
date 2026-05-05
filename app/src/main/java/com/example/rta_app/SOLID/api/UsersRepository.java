package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.entities.Users;
import com.example.rta_app.SOLID.services.ApiClient;
import com.example.rta_app.SOLID.services.TokenStorage;
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

import okhttp3.Request;
import okhttp3.RequestBody;

public class UsersRepository {
    private static final String TAG = "UsersRepo";
    private static final String FILE_NAME = "user_data.json";

    private final TokenStorage storage;
    private final Context context;
    private final ApiClient apiClient;
    private final ExecutorService executor;

    public UsersRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiClient = new ApiClient(this.context);
        this.executor = Executors.newSingleThreadExecutor();
        this.storage = new TokenStorage(this.context);
    }

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
                Log.e(TAG, "Erro ao obter usuario", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    public Task<Void> saveUser(Users user) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                String telefone = user.getTelefone() == null ? "" : user.getTelefone();
                JSONObject body = new JSONObject()
                        .put("nome", user.getName())
                        .put("telefone", telefone.replaceAll("\\D+", ""));

                executeRequest("api/motoristas/updateSite/" + user.getUid(), "PUT", body);

                String raw = readFile(FILE_NAME);
                JSONObject root = new JSONObject(raw);
                JSONObject data = root.getJSONObject("data");
                data.put("nome", user.getName());
                data.put("telefone", telefone);
                writeFile(FILE_NAME, root.toString());

                tcs.setResult(null);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao salvar usuario", e);
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    public Task<Void> loginUser(String nome, String senha) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                JSONObject loginBody = new JSONObject()
                        .put("login", nome)
                        .put("senha", senha);
                RequestBody body = RequestBody.create(loginBody.toString(), ApiClient.JSON_MEDIA);

                Request req = apiClient.request("auth/V12/login")
                        .post(body)
                        .build();

                JSONObject json = new JSONObject(apiClient.executeForBody(req));
                JSONObject data = json.optJSONObject("data");
                if (data == null) {
                    throw new IOException("Login sem dados do usuario");
                }

                String role = data.optString("role");
                if (!"MOTORISTA".equals(role)) {
                    throw new RuntimeException("Usuario nao e motorista");
                }

                String apiKey = json.getString("apiKey");
                storage.saveApiKey(apiKey);

                JSONObject root = new JSONObject().put("data", data);
                writeFile(FILE_NAME, root.toString());

                tcs.setResult(null);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao realizar login", e);
                storage.clear();
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
