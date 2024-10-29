package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.graphics.Bitmap;
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
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleDriveService {

    // Variáveis que armazenam a hora e a cidade que serão usadas para nomear o arquivo e a pasta
    private String documentName;
    private String documentCity;

    // Constantes para logging, nome do arquivo da chave da conta de serviço, e ID da pasta principal no Google Drive
    private static final String TAG = "GoogleDriveUploader";
    private static final String SERVICE_ACCOUNT_KEY_FILE = "service_account.json";
    private static final String PARENT_FOLDER_ID = "1EUOaCwgfzgGXnn6M3rIFi83IPjz_shtv";

    // Contexto da aplicação, usado para acessar recursos como o arquivo da chave de conta de serviço
    private Context context;

    // Construtor da classe que inicializa o contexto, hora e cidade
    public GoogleDriveService(Context context, String documentName, String documentCity) {
        this.context = context;
        this.documentName = documentName;
        this.documentCity = documentCity;
    }

    // Método público para iniciar o upload de um bitmap
    public void uploadBitmap(Bitmap bitmap) {
        // ExecutorService é usado para executar a tarefa de upload em uma thread separada
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Obtém uma instância do serviço do Google Drive autenticado com a conta de serviço
                Drive driveService = getDriveService(context);

                // Cria ou obtém o ID da pasta da cidade, que está dentro de uma pasta de data
                String folderId = createOrGetCityFolder(driveService);

                // Faz o upload do arquivo bitmap para a pasta obtida ou criada
                uploadFile(driveService, folderId, bitmap);

            } catch (IOException | GeneralSecurityException e) {
                // Em caso de erro, loga a mensagem de erro
                Log.e(TAG, "Erro ao enviar arquivo: " + e.getMessage());
            } finally {
                // Finaliza o ExecutorService para liberar recursos
                executor.shutdown();
            }
        });
    }

    // Método que cria ou obtém a pasta da cidade dentro da pasta de data
    private String createOrGetCityFolder(Drive driveService) throws IOException {
        String dateFolderId = getOrCreateFolder(driveService, // Obtém ou cria a pasta da data dentro da pasta principal no Google Drive
                new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()), // Cria um nome de pasta baseado na data atual no formato "dd-MM-yyyy"
                PARENT_FOLDER_ID);

        return getOrCreateFolder(driveService, documentCity, dateFolderId); // Obtém ou cria a pasta da cidade dentro da pasta de data
    }

    // Método que obtém ou cria uma pasta no Google Drive
    private String getOrCreateFolder(Drive driveService, String folderName, String parentId) throws IOException {
        String folderId = getFolderId(driveService, folderName, parentId);
        // Se a pasta não existir, cria uma nova pasta com o nome e ID do pai fornecidos
        if (folderId != null) {
            return folderId;
        }

        // Cria a pasta no Google Drive e retorna o ID da nova pasta criada
        return driveService.files()
                .create(new File()
                        .setName(folderName)
                        .setMimeType("application/vnd.google-apps.folder")// Tipo MIME para pastas no Google Drive
                        .setParents(Collections.singletonList(parentId)))// Define a pasta pai
                .setFields("id")
                .execute()
                .getId();
    }

    // Método que verifica se uma pasta já existe no Google Drive
    private String getFolderId(Drive driveService, String folderName, String parentId) throws IOException {
        // Busca a pasta pelo nome dentro do ID da pasta pai
        FileList result = driveService.files().list()
                .setQ(String.format("'%s' in parents and mimeType = 'application/vnd.google-apps.folder' and name = '%s'", parentId, folderName))
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        // Retorna o ID da pasta se existir, caso contrário, retorna null
        if (result.getFiles().isEmpty()) {
            return null;
        }

        return result.getFiles().get(0).getId();
    }

    // Método que faz o upload de um bitmap para uma pasta específica no Google Drive
    private void uploadFile(Drive driveService, String folderId, Bitmap bitmap) throws IOException {
        // Converte o bitmap para um array de bytes
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] bitmapData = bos.toByteArray();

        // Faz o upload do arquivo para o Google Drive e obtém o ID do arquivo enviado
        driveService.files()
                .create(new File() // Define os metadados do arquivo, incluindo nome, tipo MIME e pasta pai
                        .setName(documentName + ".png") // Nome do arquivo baseado na RTA
                        .setMimeType("image/png")
                        .setParents(Collections.singletonList(folderId)), // Pasta onde o arquivo será armazenado
                        new ByteArrayContent("image/png", bitmapData))
                .setFields("id")
                .execute();
    }

    // Método que configura o serviço do Google Drive usando as credenciais da conta de serviço
    private Drive getDriveService(Context context) throws GeneralSecurityException, IOException {
        // Cria uma instância do serviço do Google Drive configurada com as credenciais e retorna
        return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory
                        .getDefaultInstance(), GoogleCredential // Cria as credenciais do Google Drive com escopo de acesso ao Drive
                        .fromStream(context // Carrega o arquivo de credenciais da conta de serviço do Google Drive
                                .getAssets()
                                .open(SERVICE_ACCOUNT_KEY_FILE))
                        .createScoped(Collections
                                .singleton(DriveScopes.DRIVE_FILE)))
                .setApplicationName("RTA App") // Nome do aplicativo para identificação nas requisições do Google Drive
                .build();
    }
}
