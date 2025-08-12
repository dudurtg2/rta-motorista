package com.example.rta_app.SOLID.activitys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.databinding.ActivityLoginBinding;

import java.io.File;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG       = "LoginActivityaa";
    private static final String FILE_NAME = "user_data.json";

    private ActivityLoginBinding binding;
    private UsersRepository      usersRepository;

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        Log.d(TAG, "Camera permission callback: isGranted=" + isGranted);
                        if (isGranted) {
                            Log.i(TAG, "Permissão de câmera concedida, abrindo MainActivity");
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Log.w(TAG, "Permissão de câmera negada");
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Permissão de câmera é necessária para capturar fotos.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

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
            Log.i(TAG, "Dados salvos detectados, verificando permissão de câmera");
            checkCameraPermission();
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
                    checkCameraPermission();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "ApiLoginAccount(): falha no login", e);
                    binding.progressBarLogin.setVisibility(View.GONE);
                    Toast.makeText(
                            this,
                            "Erro ao logar: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void checkCameraPermission() {
        Log.d(TAG, "checkCameraPermission(): verificando permissão CAMERA");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permissão já concedida, abrindo MainActivity");
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        } else {
            Log.i(TAG, "Solicitando permissão de câmera");
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }
}
