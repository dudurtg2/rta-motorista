package com.example.rta_app.SOLID.activitys;

import android.Manifest;
import android.app.AlertDialog;
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

import com.example.rta_app.R;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.SOLID.services.TokenService;
import com.example.rta_app.databinding.ActivityLoginBinding;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private UsersRepository usersRepository;
    private static final String FILE_NAME = "user_data.json";

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(LoginActivity.this, "Permissão de câmera é necessária para capturar fotos.", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        this.usersRepository = new UsersRepository(this);
        asLogin();

        binding.loginButton.setOnClickListener(view -> validateData());
    }

    private void asLogin(){
        File file = getFileStreamPath(FILE_NAME);
        if (file.exists()) {
                checkCameraPermission();
            }
    }

    private void validateData() {
        String email = binding.loginEmailAddress.getText().toString().trim();
        String password = binding.loginPassword.getText().toString().trim();

        if (!email.isEmpty() && !password.isEmpty()) {
            binding.progressBarLogin.setVisibility(View.VISIBLE);
            ApiLoginAccount(email, password);
        } else {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
        }
    }

    private void ApiLoginAccount(String email, String password) {
        usersRepository.loginUser(email, password).addOnSuccessListener(aVoid -> {
            binding.progressBarLogin.setVisibility(View.GONE);
            checkCameraPermission();
        }).addOnFailureListener(e -> {
            binding.progressBarLogin.setVisibility(View.GONE);

        });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }
}
