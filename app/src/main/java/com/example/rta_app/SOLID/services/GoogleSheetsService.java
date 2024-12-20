package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.repository.WorkerHourRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.api.services.sheets.v4.model.*;

public class GoogleSheetsService {

    private static final String TAG = "GoogleSheetsService";
    private static final String SERVICE_ACCOUNT_KEY_FILE = "google-sheets.json";
    private static final String SPREADSHEET_ID = "1-MsLXdMmcjPEYSEJLv4W4YhBA0bbyHfO1Mcm-e9bCeA";
    private WorkerHous workerHous;
    private Context context;

    public GoogleSheetsService(Context context) {
        this.context = context;
    }

    public Task<Void> getGoogleSheetTask(String uid, WorkerHous workerHous) {
        // Cria uma fonte de conclusão para o Task
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        new Thread(() -> {
            try {
                Sheets sheetsService = getSheetsService(context);

                // Cria a aba caso não exista
                createSheetIfNotExists(sheetsService, uid);

                // Define o cabeçalho da aba
                setHeaderRow(sheetsService, uid);

                // Insere os dados
                ValueRange body = new ValueRange()
                        .setValues(Arrays.asList(
                                Arrays.asList(
                                        workerHous.getDate(),
                                        workerHous.getHour_first(),
                                        workerHous.getHour_dinner(),
                                        workerHous.getHour_finish(),
                                        workerHous.getHour_stop()
                                )
                        ));

                sheetsService.spreadsheets().values()
                        .append(SPREADSHEET_ID, uid + "!A2:E2", body)
                        .setValueInputOption("RAW")
                        .execute();

                // Indica que a tarefa foi concluída com sucesso
                taskCompletionSource.setResult(null);
            } catch (IOException | GeneralSecurityException e) {
                // Em caso de erro, sinaliza o Task como falhado
                Log.e(TAG, "Erro ao enviar dados para a planilha: " + e.getMessage());
                taskCompletionSource.setException(e);
            }
        }).start();

        return taskCompletionSource.getTask(); // Retorna o Task
    }

    private void createSheetIfNotExists(Sheets sheetsService, String uid) throws IOException {
        Spreadsheet spreadsheet = sheetsService.spreadsheets().get(SPREADSHEET_ID).execute();
        List<Sheet> sheets = spreadsheet.getSheets();
        boolean sheetExists = false;

        for (Sheet sheet : sheets) {
            if (sheet.getProperties().getTitle().equals(uid)) {
                sheetExists = true;
                break;
            }
        }

        if (!sheetExists) {
            BatchUpdateSpreadsheetRequest request = new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(new Request()
                            .setAddSheet(new AddSheetRequest()
                                    .setProperties(new SheetProperties()
                                            .setTitle(uid)))));

            sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID, request).execute();
            Log.i(TAG, "Nova aba criada com o UID: " + uid);
        } else {
            Log.i(TAG, "Aba com o UID já existe: " + uid);
        }
    }

    private void setHeaderRow(Sheets sheetsService, String uid) throws IOException {
        ValueRange header = new ValueRange()
                .setValues(Arrays.asList(
                        Arrays.asList("Data", "Entrada", "Almoço", "Saída", "Fim")
                ));

        sheetsService.spreadsheets().values()
                .update(SPREADSHEET_ID, uid + "!A1:E1", header)
                .setValueInputOption("RAW")
                .execute();

        Log.i(TAG, "Cabeçalhos definidos para a aba: " + uid);
    }

    private Sheets getSheetsService(Context context) throws GeneralSecurityException, IOException {
        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(), GoogleCredential
                .fromStream(context.getAssets().open(SERVICE_ACCOUNT_KEY_FILE))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/spreadsheets")))
                .setApplicationName("RTA App")
                .build();
    }
}
