package com.example.rta_app.Fuctions.DAO.Driver;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

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
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class GoogleDriveUploader {
    private String hour;
    private String City;
    private static final String TAG = "GoogleDriveUploader";
    private static final String SERVICE_ACCOUNT_KEY_FILE = "service_account.json"; // Nome do arquivo JSON
    private static final String PARENT_FOLDER_ID = "1EUOaCwgfzgGXnn6M3rIFi83IPjz_shtv"; // ID da pasta principal, se necessário

    private Context context;

    public GoogleDriveUploader(Context context, String hour, String City) {
        this.context = context;
        this.hour = hour;
        this.City = City;
    }

    public void uploadBitmap(Bitmap bitmap) {
        new UploadTask().execute(bitmap);
    }

    private class UploadTask extends AsyncTask<Bitmap, Void, Void> {
        @Override
        protected Void doInBackground(Bitmap... bitmaps) {
            Bitmap bitmap = bitmaps[0];
            try {
                Drive driveService = getDriveService(context);

                // Crie a estrutura de pastas: data -> City
                String folderId = createOrGetCityFolder(driveService);

                // Faça o upload da imagem para a pasta criada
                uploadFile(driveService, folderId, bitmap);

            } catch (IOException | GeneralSecurityException e) {
                Log.e(TAG, "Erro ao enviar arquivo: " + e.getMessage());
            }
            return null;
        }

        private String createOrGetCityFolder(Drive driveService) throws IOException {
            String dateFolderName = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            String cityFolderName = City;

            String dateFolderId = getOrCreateFolder(driveService, dateFolderName, PARENT_FOLDER_ID);

            return getOrCreateFolder(driveService, cityFolderName, dateFolderId);
        }

        private String getOrCreateFolder(Drive driveService, String folderName, String parentId) throws IOException {

            String folderId = getFolderId(driveService, folderName, parentId);
            if (folderId != null) {
                return folderId;
            }


            File fileMetadata = new File();
            fileMetadata.setName(folderName);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            fileMetadata.setParents(Collections.singletonList(parentId));

            File folder = driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();

            Log.d(TAG, "Pasta criada com sucesso! ID da pasta: " + folder.getId());
            return folder.getId();
        }

        private String getFolderId(Drive driveService, String folderName, String parentId) throws IOException {
            FileList result = driveService.files().list()
                    .setQ(String.format("'%s' in parents and mimeType = 'application/vnd.google-apps.folder' and name = '%s'", parentId, folderName))
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute();

            if (result.getFiles().isEmpty()) {
                return null;
            }

            return result.getFiles().get(0).getId();
        }

        private void uploadFile(Drive driveService, String folderId, Bitmap bitmap) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            byte[] bitmapData = bos.toByteArray();

            File fileMetadata = new File();
            fileMetadata.setName(hour + ".png");
            fileMetadata.setMimeType("image/png");
            fileMetadata.setParents(Collections.singletonList(folderId));

            ByteArrayContent content = new ByteArrayContent("image/png", bitmapData);

            File file = driveService.files().create(fileMetadata, content)
                    .setFields("id")
                    .execute();
            Log.d(TAG, "Arquivo enviado com sucesso! ID do arquivo: " + file.getId());
        }
    }

    private Drive getDriveService(Context context) throws GeneralSecurityException, IOException {
        InputStream credentialsStream = context.getAssets().open(SERVICE_ACCOUNT_KEY_FILE);

        GoogleCredential credential = GoogleCredential.fromStream(credentialsStream)
                .createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("Seu aplicativo")
                .build();
    }
}
