package com.example.rta_app.SOLID.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.example.rta_app.SOLID.Interfaces.IWorkerHourRepository;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

public class WorkerHourRepository implements IWorkerHourRepository {

    private Context context;
    private static final String FILE_NAME = "worker_hours.json";

    public WorkerHourRepository(Context context) {
        this.context = context;
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
}
