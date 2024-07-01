package com.example.lc_app.Activitys.User;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.lc_app.Activitys.MainActivity;
import com.example.lc_app.R;
import com.example.lc_app.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private DocumentReference docRef;
    private FirebaseFirestore firestore;
    private static final int REQUEST_CAMERA_PERMISSION = 101;

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Permissão de câmera é necessária para capturar fotos.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        firestore = FirebaseFirestore.getInstance();

        mAuth = FirebaseAuth.getInstance();
        binding.loginButton.setOnClickListener(view -> validateData());
    }

    private void validateData() {
        String email = binding.loginEmailAddress.getText().toString().trim();
        String password = binding.loginPassword.getText().toString().trim();
        if (!email.isEmpty() && !password.isEmpty()) {
            FireBaseLoginAccount(email, password);
        } else {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
        }
    }

    private void FireBaseLoginAccount(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkStoreData();
                    } else {
                        Toast.makeText(this, "Email não cadastrado", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void checkStoreData(){
        docRef = firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid());
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                HashMap<String, Object> data = new HashMap<>();
                data.put("nome", mAuth.getCurrentUser().getDisplayName());
                data.put("uid", mAuth.getCurrentUser().getUid());
                data.put("rota", "001");

                docRef.set(data).addOnSuccessListener(aVoid -> {
                    checkCameraPermission();
                });
            } else {
                checkCameraPermission();
            }
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
