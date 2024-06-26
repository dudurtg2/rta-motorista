package com.example.lc_app.Activitys.User;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lc_app.Fuctions.DAO.User.ImageUploaderDAO;
import com.example.lc_app.Fuctions.DAO.User.UserDAO;
import com.example.lc_app.R;
import com.example.lc_app.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {
    public ActivityProfileBinding binding;
    private DocumentReference docRef;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private ImageUploaderDAO imageUploader;
    public static final int PICK_IMAGE_REQUEST = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.profileUserInsert.setVisibility(View.GONE);

        imageUploader = new ImageUploaderDAO(this);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            docRef = firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid());
        } else {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.editNameUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    binding.profileUserInsert.setVisibility(View.VISIBLE);
                } else {
                    binding.profileUserInsert.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        imageUploader.loadImagem();
        getUser();

        binding.UserImagenView.setOnClickListener(v -> imageUploader.openFileChooser(this));
        binding.profileUserInsert.setOnClickListener(v -> updateUser());

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imageUploader.handleImageResult(requestCode, resultCode, data, this);
    }
    private void getUser() {
        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        binding.editNameUser.setHint(documentSnapshot.getString("nome"));
                    } else {
                        binding.editNameUser.setHint(mAuth.getCurrentUser().getDisplayName());
                    }
                })
                .addOnFailureListener(e -> { Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show(); });
    }

    private void updateUser() {
        if (mAuth.getCurrentUser() != null) {
            new UserDAO(this)
                    .addUser(mAuth.getCurrentUser().getUid(), binding.editNameUser.getText().toString())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Usuário atualizado com sucesso", Toast.LENGTH_SHORT).show();
                        binding.profileUserInsert.setVisibility(View.GONE);
                        binding.editNameUser.setText("");
                    })
                    .addOnFailureListener(e -> { Toast.makeText(this, "Erro ao atualizar o usuário", Toast.LENGTH_SHORT).show(); });
        }
    }
}