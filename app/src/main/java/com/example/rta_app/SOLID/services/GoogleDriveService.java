package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.rta_app.SOLID.Interfaces.IPackingListRepository;
import com.example.rta_app.SOLID.api.PackingListRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleDriveService {

    private String documentName;
    private String documentCity;
    private IPackingListRepository packingListRepository;

    private static final String TAG = "RTAAPITESTGOOGLE";
    private static final String SERVICE_ACCOUNT_KEY_FILE = "service_account.json";
    private static final String PARENT_FOLDER_ID = "1EUOaCwgfzgGXnn6M3rIFi83IPjz_shtv";

    private Context context;

    public GoogleDriveService(Context context, String documentName, String documentCity) {
        this.context = context;
        this.documentName = documentName;
        this.documentCity = documentCity;
        this.packingListRepository = new PackingListRepository(context);
    }

    public void uploadBitmap(Bitmap bitmap) {
        Log.i(TAG, "Iniciando o upload da imagem...");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Log.i(TAG, "Obtendo o serviço do Google Drive...");
                Drive driveService = getDriveService(context);

                Log.i(TAG, "Criando ou buscando a pasta da cidade...");
                String folderId = createOrGetCityFolder(driveService);

                Log.i(TAG, "Fazendo o upload do arquivo...");
                String publicLink = uploadFile(driveService, folderId, bitmap);
                Log.i(TAG, "Link público gerado: " + publicLink);

                packingListRepository.updateImgLinkForFinish(documentName, publicLink)
                        .addOnSuccessListener(v -> Log.i(TAG, "Link público da imagem atualizado com sucesso: " + publicLink))
                        .addOnFailureListener(vv -> Log.e(TAG, "Erro ao atualizar link público: " + vv.getMessage()));

            } catch (IOException | GeneralSecurityException e) {
                Log.e(TAG, "Erro ao enviar arquivo: " + e.getMessage(), e);
            } finally {
                Log.i(TAG, "Finalizando a execução da thread.");
                executor.shutdown();
            }
        });
    }

    private String createOrGetCityFolder(Drive driveService) throws IOException {
        Log.i(TAG, "Buscando ou criando a pasta para a data...");
        String dateFolderId = getOrCreateFolder(driveService,
                new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()),
                PARENT_FOLDER_ID);

        Log.i(TAG, "Buscando ou criando a pasta para a cidade...");
        return getOrCreateFolder(driveService, documentCity, dateFolderId);
    }

    private String getOrCreateFolder(Drive driveService, String folderName, String parentId) throws IOException {
        Log.i(TAG, "Procurando pasta: " + folderName);
        String folderId = getFolderId(driveService, folderName, parentId);
        if (folderId != null) {
            Log.i(TAG, "Pasta encontrada: " + folderId);
            return folderId;
        }

        Log.i(TAG, "Pasta não encontrada, criando nova pasta...");
        File folder = driveService.files()
                .create(new File()
                        .setName(folderName)
                        .setMimeType("application/vnd.google-apps.folder")
                        .setParents(Collections.singletonList(parentId)))
                .setFields("id")
                .execute();

        Log.i(TAG, "Pasta criada: " + folder.getId());
        return folder.getId();
    }

    private String getFolderId(Drive driveService, String folderName, String parentId) throws IOException {
        Log.i(TAG, "Procurando folder no Google Drive com o nome: " + folderName);
        FileList result = driveService.files().list()
                .setQ(String.format("'%s' in parents and mimeType = 'application/vnd.google-apps.folder' and name = '%s'", parentId, folderName))
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        if (result.getFiles().isEmpty()) {
            Log.i(TAG, "Nenhuma pasta encontrada.");
            return null;
        }

        Log.i(TAG, "Pasta encontrada: " + result.getFiles().get(0).getId());
        return result.getFiles().get(0).getId();
    }

    private String uploadFile(Drive driveService, String folderId, Bitmap bitmap) throws IOException {
        Log.i(TAG, "Iniciando o processo de upload do arquivo.");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] bitmapData = bos.toByteArray();

        Log.i(TAG, "Compressão da imagem concluída, tamanho: " + bitmapData.length + " bytes.");

        File uploadedFile = driveService.files()
                .create(new File()
                                .setName(documentName + ".png")
                                .setMimeType("image/png")
                                .setParents(Collections.singletonList(folderId)),
                        new ByteArrayContent("image/png", bitmapData))
                .setFields("id, webViewLink, webContentLink")
                .execute();

        Log.i(TAG, "Arquivo enviado, ID: " + uploadedFile.getId());

        driveService.permissions().create(uploadedFile.getId(),
                        new com.google.api.services.drive.model.Permission()
                                .setType("anyone")
                                .setRole("reader"))
                .execute();

        Log.i(TAG, "Permissões de leitura pública aplicadas.");

        return uploadedFile.getWebViewLink();
    }

    private Drive getDriveService(Context context) throws GeneralSecurityException, IOException {
        Log.i(TAG, "Criando o serviço Google Drive...");
        return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(), GoogleCredential
                .fromStream(context.getAssets()
                        .open(SERVICE_ACCOUNT_KEY_FILE))
                .createScoped(Collections.singleton(DriveScopes.DRIVE_FILE)))
                .setApplicationName("RTA App")
                .build();
    }
}
