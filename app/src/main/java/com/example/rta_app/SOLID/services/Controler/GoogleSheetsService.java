package com.example.rta_app.SOLID.services.Controler;

import android.content.Context;
import android.util.Log;

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
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private static final String TAG = "GoogleSheetsService";
    private static final String SERVICE_ACCOUNT_KEY_FILE = "google-sheets.json"; // Nome do arquivo da conta de serviço
    private static final String SPREADSHEET_ID = "1-MsLXdMmcjPEYSEJLv4W4YhBA0bbyHfO1Mcm-e9bCeA";

    private Context context;

    public GoogleSheetsService(Context context) {
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        this.context = context;
    }

    public void getGoogleSheet(String uid) {
        firestore.collection("usuarios")
                .document(mAuth.getCurrentUser().getUid())
                .collection("work_hours")
                .document("cachehoras")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        sendToGoogleSheet(
                                uid,
                                documentSnapshot.getString("dia_mes_ano"),
                                documentSnapshot.getString("Entrada"),
                                documentSnapshot.getString("Almoço"),
                                documentSnapshot.getString("Saída"),
                                documentSnapshot.getString("Fim")
                        );
                    }
                });
    }

    public void sendToGoogleSheet(String uid ,String data, String entrada, String almoco, String saida, String fim) {
        new Thread(() -> {
            try {
                Sheets sheetsService = getSheetsService(context);


                createSheetIfNotExists(sheetsService, uid);

                setHeaderRow(sheetsService, uid);

                ValueRange body = new ValueRange()
                        .setValues(Arrays.asList(
                                Arrays.asList(data, entrada, almoco, saida, fim)
                        ));

                sheetsService.spreadsheets().values()
                        .append(SPREADSHEET_ID, uid + "!A2:E2", body)
                        .setValueInputOption("RAW")
                        .execute();

                Log.i(TAG, "Dados enviados para a planilha com sucesso!");
            } catch (IOException | GeneralSecurityException e) {
                Log.e(TAG, "Erro ao enviar dados para a planilha: " + e.getMessage());
            }
        }).start();
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
                        Arrays.asList("Data", "Entrada", "Almoço", "Saída", "Fim") // Cabeçalhos
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

