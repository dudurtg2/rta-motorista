package com.example.rta_app.SOLID.activitys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.rta_app.SOLID.activitys.tracker.LocationTracker;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.databinding.ActivityLoginBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG       = "LoginActivityaa";
    private static final String FILE_NAME = "user_data.json";

    private ActivityLoginBinding binding;
    private UsersRepository      usersRepository;

    // ======== PERMISSION LAUNCHERS ========

    // Pede Câmera + Localização (fine/coarse) + Notificações (Android 13+) de uma vez
    private final ActivityResultLauncher<String[]> requestAllPermsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (Map<String, Boolean> result) -> {
                Log.d(TAG, "Result multiple perms: " + result);

                boolean camGranted    = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.CAMERA, false));
                boolean fineGranted   = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false));
                boolean coarseGranted = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false));
                boolean notifGranted  = (Build.VERSION.SDK_INT < 33)
                        || Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false));

                if (!camGranted || !(fineGranted || coarseGranted) || !notifGranted) {
                    Log.w(TAG, "Algumas permissões foram negadas (camera/location/notifications).");
                    Toast.makeText(this, "Conceda Câmera, Localização e Notificações para continuar.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Base OK → verificar BACKGROUND (Android 10+ exige etapa separada)
                if (needsBackgroundLocation() && !hasBackgroundLocation()) {
                    maybeExplainBackgroundThenRequest();
                } else {
                    openMain();
                }
            });

    // BACKGROUND LOCATION deve ser pedida separadamente (Android 10+)
    private final ActivityResultLauncher<String> requestBackgroundLocationLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                Log.d(TAG, "Background location granted? " + isGranted);
                if (!isGranted) {
                    Toast.makeText(this, "Permita 'Localização em 2º plano' para rastrear mesmo com o app fechado.", Toast.LENGTH_LONG).show();
                }
                openMain();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate(): início");

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "Layout inflado e definido");

        usersRepository = new UsersRepository(this);
        Log.d(TAG, "UsersRepository inicializado");

        binding.loginButton.setOnClickListener(view -> {
            Log.d(TAG, "Login button clicked");
            validateData();
        });
    }

    private void asLogin() {
        Log.d(TAG, "asLogin(): verificando existência de " + FILE_NAME);
        File file = getFileStreamPath(FILE_NAME);
        boolean exists = file.exists();
        Log.d(TAG, FILE_NAME + " existe? " + exists);
        if (exists) {
            Log.i(TAG, "Dados salvos detectados, verificando permissões necessárias");
            checkAndRequestAllPermissions();
        }
    }

    private void validateData() {
        String email    = binding.loginEmailAddress.getText().toString().trim();
        String password = binding.loginPassword.getText().toString().trim();
        Log.d(TAG, "validateData(): email=\"" + email + "\", password length=" + password.length());

        if (!email.isEmpty() && !password.isEmpty()) {
            Log.i(TAG, "Campos preenchidos, iniciando login API");
            binding.progressBarLogin.setVisibility(View.VISIBLE);
            ApiLoginAccount(email, password);
        } else {
            Log.w(TAG, "validateData(): campos faltando");
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
        }
    }

    private void ApiLoginAccount(String email, String password) {
        Log.d(TAG, "ApiLoginAccount(): email=" + email);
        usersRepository.loginUser(email, password)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "ApiLoginAccount(): login bem-sucedido");
                    binding.progressBarLogin.setVisibility(View.GONE);
                    // Depois do login → peça todas as permissões
                    checkAndRequestAllPermissions();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "ApiLoginAccount(): falha no login", e);
                    binding.progressBarLogin.setVisibility(View.GONE);
                    Toast.makeText(this, "Erro ao logar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ========= NOVO FLUXO DE PERMISSÕES =========

    private void checkAndRequestAllPermissions() {
        Log.d(TAG, "checkAndRequestAllPermissions()");
        if (hasAllBasePermissions()) {
            // Base OK → tratar background se necessário
            if (needsBackgroundLocation() && !hasBackgroundLocation()) {
                maybeExplainBackgroundThenRequest();
            } else {
                openMain();
            }
            return;
        }

        // Monta lista base: câmera + localização; notificações se Android 13+
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        requestAllPermsLauncher.launch(perms.toArray(new String[0]));
    }

    private boolean hasAllBasePermissions() {
        boolean cam    = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean fine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean notif  = (Build.VERSION.SDK_INT < 33)
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        return cam && (fine || coarse) && notif;
    }

    private boolean needsBackgroundLocation() {
        return Build.VERSION.SDK_INT >= 29; // Android 10+
    }

    private boolean hasBackgroundLocation() {
        if (Build.VERSION.SDK_INT < 29) return true; // antes do Android 10 não tinha separada
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void maybeExplainBackgroundThenRequest() {
        boolean shouldExplain =
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        if (shouldExplain) {
            new AlertDialog.Builder(this)
                    .setTitle("Permitir localização em 2º plano")
                    .setMessage("Para rastrear mesmo com o app fechado, permita 'Localização o tempo todo' na próxima tela.")
                    .setPositiveButton("Continuar", (d, w) ->
                            requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    )
                    .setNegativeButton("Agora não", (d, w) -> openMain())
                    .show();
        } else {
            requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
    }

    // ========= NAVEGAÇÃO =========

    private void openMain() {
        LocationTracker.start(this);
        Log.i(TAG, "Abrindo MainActivity");
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    // Mantém o método antigo por compatibilidade e delega para o novo fluxo
    private void checkCameraPermission() {
        Log.d(TAG, "checkCameraPermission(): delegando para checkAndRequestAllPermissions()");
        checkAndRequestAllPermissions();
    }
}
