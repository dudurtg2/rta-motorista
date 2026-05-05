package com.example.rta_app.SOLID.activitys;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.rta_app.SOLID.activitys.tracker.LocationTracker;
import com.example.rta_app.SOLID.api.AppRepository;
import com.example.rta_app.SOLID.entities.App;
import com.example.rta_app.SOLID.services.NetworkService;
import com.example.rta_app.SOLID.services.TokenStorage;
import com.example.rta_app.databinding.ActivityFirstBinding;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirstActivity extends AppCompatActivity {
    private static final String TAG = "FirstActivity";

    private static final String VERSSION_APP = "1.2.0";

    private final ExecutorService startupExecutor = Executors.newSingleThreadExecutor();

    private ActivityFirstBinding binding;
    private TokenStorage tokenStorage;
    private NetworkService networkService;
    private AppRepository appRepository;

    private long downloadId = -1;
    private ProgressDialog progressDialog;
    private File downloadedApkFile;

    private boolean installStarted = false;
    private boolean waitingInstallPermission = false;
    private boolean receiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFirstBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenStorage = new TokenStorage(getApplicationContext());
        networkService = new NetworkService();
        appRepository = new AppRepository(getApplicationContext());

        TokenStorage.warmUpAsync(getApplicationContext());
        checkAppVersionThenContinue();
    }

    private void checkAppVersionThenContinue() {
        boolean connected = networkService.isNetworkConnected(this);
        Log.i(TAG, "isNetworkConnected=" + connected);

        if (!connected) {
            Toast.makeText(this, "Sem conexão com a internet", Toast.LENGTH_SHORT).show();
            startWorkHour();
            return;
        }

        appRepository.getLatestApp()
                .addOnSuccessListener(app -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (app == null) {
                        Log.w(TAG, "App mais recente veio nulo; seguindo fluxo normal");
                        decideNextScreen();
                        return;
                    }

                    String latestVersion = app.getVersao() == null ? "" : app.getVersao().trim();

                    if (latestVersion.equals(VERSSION_APP)) {
                        decideNextScreen();
                    } else {
                        showUpdateDialog(app);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Falha ao verificar versao do app; seguindo fluxo normal", e);

                    if (!isFinishing() && !isDestroyed()) {
                        decideNextScreen();
                    }
                });
    }

    private void decideNextScreen() {
        startupExecutor.execute(() -> {
            String apiKey = tokenStorage.getApiKey();

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (apiKey.isEmpty()) {
                    Log.i(TAG, "Token ausente; abrindo LoginActivity");
                    startLogin();
                } else {
                    Log.i(TAG, "Token presente; iniciando rastreamento e abrindo MainActivity");
                    startMain();
                }
            });
        });
    }

    private void showUpdateDialog(App app) {
        String version = app.getVersao() == null || app.getVersao().trim().isEmpty()
                ? "mais recente"
                : app.getVersao().trim();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Atualização disponível")
                .setMessage("Existe uma versão " + version + " disponível para este app.")
                .setCancelable(false)
                .setPositiveButton("Atualizar", (dialog, which) -> downloadAndInstallApk(app.getLink()))
                .show();
    }

    private void downloadAndInstallApk(String link) {
        installStarted = false;
        waitingInstallPermission = false;

        if (link == null || link.trim().isEmpty()) {
            Toast.makeText(this, "Link de atualização indisponível", Toast.LENGTH_LONG).show();
            return;
        }

        String apkUrl = normalizeDropboxUrl(link.trim());

        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

            if (downloadManager == null) {
                Toast.makeText(this, "Gerenciador de download indisponível", Toast.LENGTH_LONG).show();
                return;
            }

            String fileName = "rta_app_update.apk";

            File downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

            if (downloadsDir == null) {
                Toast.makeText(this, "Pasta de download indisponível", Toast.LENGTH_LONG).show();
                return;
            }

            downloadedApkFile = new File(downloadsDir, fileName);

            if (downloadedApkFile.exists()) {
                boolean deleted = downloadedApkFile.delete();
                Log.i(TAG, "APK antigo removido=" + deleted);
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("Atualizando aplicativo");
            request.setDescription("Baixando nova versão...");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setDestinationUri(Uri.fromFile(downloadedApkFile));
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            downloadId = downloadManager.enqueue(request);

            registerDownloadReceiver();
            showDownloadProgress(downloadManager);

        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar download da atualização", e);
            Toast.makeText(this, "Não foi possível iniciar o download", Toast.LENGTH_LONG).show();
        }
    }

    private String normalizeDropboxUrl(String url) {
        if (!url.contains("dropbox.com")) {
            return url;
        }

        if (url.contains("dl=1")) {
            return url;
        }

        if (url.contains("dl=0")) {
            return url.replace("dl=0", "dl=1");
        }

        if (url.contains("?")) {
            return url + "&dl=1";
        }

        return url + "?dl=1";
    }

    private void registerDownloadReceiver() {
        if (receiverRegistered) {
            return;
        }

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadCompleteReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadCompleteReceiver, filter);
        }

        receiverRegistered = true;
    }

    private void showDownloadProgress(DownloadManager downloadManager) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Atualizando");
        progressDialog.setMessage("Baixando atualização...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            boolean downloading = true;

            while (downloading) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);

                try (Cursor cursor = downloadManager.query(query)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                        int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);

                        int status = cursor.getInt(statusIndex);
                        long downloaded = cursor.getLong(downloadedIndex);
                        long total = cursor.getLong(totalIndex);

                        if (total > 0) {
                            int progress = (int) ((downloaded * 100L) / total);

                            runOnUiThread(() -> {
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.setProgress(progress);
                                }
                            });
                        }

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false;

                            runOnUiThread(() -> {
                                if (installStarted) {
                                    return;
                                }

                                installStarted = true;

                                dismissProgressDialog();
                                unregisterDownloadReceiverSafely();
                                installDownloadedApk();
                            });

                        } else if (status == DownloadManager.STATUS_FAILED) {
                            downloading = false;

                            int reason = reasonIndex >= 0 ? cursor.getInt(reasonIndex) : -1;
                            Log.e(TAG, "Download da atualização falhou. reason=" + reason);

                            runOnUiThread(() -> {
                                dismissProgressDialog();
                                unregisterDownloadReceiverSafely();

                                Toast.makeText(
                                        this,
                                        "Falha ao baixar atualização",
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao consultar progresso do download", e);
                    downloading = false;

                    runOnUiThread(() -> {
                        dismissProgressDialog();
                        unregisterDownloadReceiverSafely();

                        Toast.makeText(
                                this,
                                "Erro ao acompanhar download",
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    downloading = false;
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private final BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            if (completedId != downloadId) {
                return;
            }

            if (installStarted) {
                return;
            }

            installStarted = true;

            dismissProgressDialog();
            unregisterDownloadReceiverSafely();
            installDownloadedApk();
        }
    };

    private void installDownloadedApk() {
        if (downloadedApkFile == null || !downloadedApkFile.exists()) {
            installStarted = false;
            waitingInstallPermission = false;

            Toast.makeText(this, "Arquivo de atualização não encontrado", Toast.LENGTH_LONG).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !getPackageManager().canRequestPackageInstalls()) {

            waitingInstallPermission = true;
            installStarted = false;

            Toast.makeText(this, "Autorize a instalação deste app", Toast.LENGTH_LONG).show();

            Intent settingsIntent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName())
            );

            startActivity(settingsIntent);
            return;
        }

        try {
            Uri apkUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    downloadedApkFile
            );

            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            waitingInstallPermission = false;
            startActivity(installIntent);

        } catch (Exception e) {
            installStarted = false;
            waitingInstallPermission = false;

            Log.e(TAG, "Erro ao abrir instalador do APK", e);
            Toast.makeText(this, "Não foi possível abrir o instalador", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!waitingInstallPermission) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !getPackageManager().canRequestPackageInstalls()) {
            return;
        }

        waitingInstallPermission = false;

        if (downloadedApkFile != null && downloadedApkFile.exists()) {
            installStarted = true;
            installDownloadedApk();
        } else {
            installStarted = false;
            Toast.makeText(this, "Arquivo de atualização não encontrado", Toast.LENGTH_LONG).show();
        }
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void unregisterDownloadReceiverSafely() {
        if (!receiverRegistered) {
            return;
        }

        try {
            unregisterReceiver(downloadCompleteReceiver);
        } catch (Exception ignored) {
        }

        receiverRegistered = false;
    }

    private void startMain() {
        LocationTracker.sync(getApplicationContext());

        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    private void startLogin() {
        Intent intent = new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    private void startWorkHour() {
        Intent intent = new Intent(this, WorkHourActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        startupExecutor.shutdownNow();

        dismissProgressDialog();
        unregisterDownloadReceiverSafely();
    }
}