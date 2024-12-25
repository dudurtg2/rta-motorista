package com.example.rta_app.SOLID.api;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.example.rta_app.SOLID.Interfaces.IWorkerHourRepository;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.services.TokenService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WorkerHourRepository implements IWorkerHourRepository {

    private Context context;
    private static final String TAG = "WorkerHourRepository";
    private static final String FILE_NAME = "worker_hours.json";
    private static final String FILE_NAME_USER = "user_data.json";
    private static final String URL_API = "http://carlo4664.c44.integrator.host:10500/";
    private TokenService tokenService;

    public WorkerHourRepository(Context context) {
        this.context = context;
        this.tokenService = new TokenService(context);
    }

    public Task<Void> saveWorkerHous(WorkerHous workerHous) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("date", workerHous.getDate());
            jsonObject.put("hour_first", workerHous.getHour_first());
            jsonObject.put("hour_dinner", workerHous.getHour_dinner());
            jsonObject.put("hour_finish", workerHous.getHour_finish());
            jsonObject.put("hour_stop", workerHous.getHour_stop());
            jsonObject.put("hour_after", workerHous.getHour_after());

            writeToFile(jsonObject.toString());
            taskCompletionSource.setResult(null);

        } catch (JSONException | IOException e) {
            Log.e("WorkerHourRepository", "Erro ao salvar WorkerHous", e);
            taskCompletionSource.setException(e);
        }

        return taskCompletionSource.getTask();
    }

    public Task<WorkerHous> getWorkerHous() {
        TaskCompletionSource<WorkerHous> taskCompletionSource = new TaskCompletionSource<>();

        try {
            String jsonData = readFromFile();

            if (jsonData == null) {
                taskCompletionSource.setResult(new WorkerHous("", "", "", "", "", ""));
            } else {
                JSONObject jsonObject = new JSONObject(jsonData);

                String data = jsonObject.optString("date", "");
                String entrada = jsonObject.optString("hour_first", "");
                String almoco = jsonObject.optString("hour_dinner", "");
                String saida = jsonObject.optString("hour_finish", "");
                String fim = jsonObject.optString("hour_stop", "");
                String anterior = jsonObject.optString("hour_after", "");

                WorkerHous workerHous = new WorkerHous(data, entrada, almoco, saida, fim, anterior);
                taskCompletionSource.setResult(workerHous);
            }
        } catch (JSONException | IOException e) {
            Log.e("WorkerHourRepository", "Erro ao ler WorkerHous", e);
            taskCompletionSource.setException(e);
        }

        return taskCompletionSource.getTask();
    }

    public void writeToFile(String data) throws IOException {
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(data.getBytes());
        }
    }

    public String readFromFile() throws IOException {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            return null;
        }

        try (FileInputStream fis = context.openFileInput(FILE_NAME)) {
            StringBuilder stringBuilder = new StringBuilder();
            int content;
            while ((content = fis.read()) != -1) {
                stringBuilder.append((char) content);
            }
            return stringBuilder.toString();
        }
    }
    private String readFile(String fileName) throws IOException {
        try (FileInputStream fis = context.openFileInput(fileName)) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }
    private String getAccessTokenFromLocalFile() {
        try {
            String jsonContent = readFile(FILE_NAME_USER);
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
    private String getIdDriveFromLocalFile() {

        try {
            String jsonContent = readFile(FILE_NAME_USER);
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
    @Override

    public Task<Void> saveHors(WorkerHous workerHous) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        tokenService.validateAndRefreshToken();

        Log.i(TAG, workerHous.toString());

        // Criação do JSON
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("date", workerHous.getDate());
            jsonBody.put("hour_first", workerHous.getHour_first());
            jsonBody.put("hour_dinner", workerHous.getHour_dinner());
            jsonBody.put("hour_finish", workerHous.getHour_finish());
            jsonBody.put("hour_stop", workerHous.getHour_stop());
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao criar JSON", e);
            taskCompletionSource.setException(e);
            return taskCompletionSource.getTask();
        }

        Log.i(TAG, "JSON enviado: " + jsonBody.toString());

        // Criação do corpo da requisição
        String accessToken = getAccessTokenFromLocalFile();
        RequestBody body = RequestBody.create(
                jsonBody.toString(), MediaType.get("application/json; charset=utf-8")
        );

        Log.i(TAG, "RequestBody criado: " + body.toString());

        // Construção da requisição HTTP
        Request request = new Request.Builder()
                .url(URL_API + "api/pontos/save/" + getIdDriveFromLocalFile())
                .post(body)
                .addHeader("Authorization", "Bearer " + accessToken) // Adiciona o token de autenticação
                .build();

        // Execução da requisição em uma nova thread
        new Thread(() -> {
            try (Response response = new OkHttpClient().newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : null;

                if (response.isSuccessful()) {
                    Log.d(TAG, "Requisição bem-sucedida: " + responseBody);
                    taskCompletionSource.setResult(null);
                } else {
                    Log.e(TAG, "Erro na requisição: " + responseBody);
                    taskCompletionSource.setException(
                            new Exception("Erro na API: " + responseBody)
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao enviar a requisição", e);
                taskCompletionSource.setException(e);
            }
        }).start();

        return taskCompletionSource.getTask();
    }

}
