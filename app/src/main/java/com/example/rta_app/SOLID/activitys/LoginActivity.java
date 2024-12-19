package com.example.rta_app.SOLID.activitys;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.rta_app.R;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.databinding.ActivityLoginBinding;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private UsersRepository usersRepository; // Repositório inicializado no onCreate

    // Inicializador para a permissão de câmera
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

        // Inicializando o repositório após o ciclo de vida ter iniciado
        usersRepository = new UsersRepository(this);

        // Configura o botão de login
        binding.loginButton.setOnClickListener(view -> validateData());
    }

    private void validateData() {
        // Obtenção dos dados de email e senha
        String email = binding.loginEmailAddress.getText().toString().trim();
        String password = binding.loginPassword.getText().toString().trim();

        // Verifica se os campos não estão vazios
        if (!email.isEmpty() && !password.isEmpty()) {
            binding.progressBarLogin.setVisibility(View.VISIBLE); // Mostra a barra de progresso
            ApiLoginAccount(email, password); // Chama o método de login
        } else {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
        }
    }

    private void ApiLoginAccount(String email, String password) {
        // Faz login utilizando o repositório de usuários
        usersRepository.loginUser(email, password).addOnSuccessListener(aVoid -> {
            binding.progressBarLogin.setVisibility(View.GONE); // Oculta a barra de progresso em caso de sucesso
            checkCameraPermission(); // Verifica a permissão da câmera
        }).addOnFailureListener(e -> {
            binding.progressBarLogin.setVisibility(View.GONE);
            new AlertDialog.Builder(this)
                    .setTitle("ERRO")
                    .setMessage(e.getMessage())
                    .setNegativeButton("Não", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void checkCameraPermission() {
        // Verifica se a permissão de câmera já foi concedida
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        } else {
            // Solicita a permissão de câmera
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }
}
