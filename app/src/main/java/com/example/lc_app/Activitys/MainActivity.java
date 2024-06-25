package com.example.lc_app.Activitys;

import static android.content.ContentValues.TAG;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lc_app.Fuctions.DAO.ImageUploaderDAO;
import com.example.lc_app.Fuctions.DAO.UserDAO;
import com.example.lc_app.R;
import com.example.lc_app.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public ActivityMainBinding binding;
    private DocumentReference docRef;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private ImageUploaderDAO imageUploader;
    public static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
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
        SimpleDateFormat formatter2 = new SimpleDateFormat("HH:mm");
        String timeString = formatter2.format(new Date());

        imageUploader.loadImagem();
        getUser();

        binding.UserImagenView.setOnClickListener(v -> imageUploader.openFileChooser(this));

        binding.profileUserInsert.setOnClickListener(v -> updateUser());
        binding.buttonFistHour.setOnClickListener(v -> {
            UpdateWorkHours("Entrada").addOnSuccessListener(Void -> binding.buttonFistHour.setText(timeString));
        });
        binding.buttonDinnerStarHour.setOnClickListener(v -> {
            UpdateWorkHours("Almoço").addOnSuccessListener(Void -> binding.buttonDinnerStarHour.setText(timeString));
        });
        binding.buttonDinnerFinishHour.setOnClickListener(v -> {
            UpdateWorkHours("Saída").addOnSuccessListener(Void -> binding.buttonDinnerFinishHour.setText(timeString));
        });
        binding.buttonStop.setOnClickListener(v -> {
            UpdateWorkHours("Fim").addOnSuccessListener(Void -> binding.buttonStop.setText(timeString));
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imageUploader.handleImageResult(requestCode, resultCode, data, this);
    }

    private Task<Void> UpdateWorkHours(String horario) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        String dateString = formatter.format(new Date());
        SimpleDateFormat formatter2 = new SimpleDateFormat("HH:mm");
        String timeString = formatter2.format(new Date());

        return new UserDAO(this).updateWorkHours(mAuth.getCurrentUser().getUid(), dateString, timeString, horario);
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
                .addOnFailureListener(e -> { Toast.makeText(MainActivity.this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show(); });
    }

    private void updateUser() {
        if (mAuth.getCurrentUser() != null) {
            new UserDAO(this)
                    .addUser(mAuth.getCurrentUser().getUid(), binding.editNameUser.getText().toString())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, "Usuário atualizado com sucesso", Toast.LENGTH_SHORT).show();
                        binding.profileUserInsert.setVisibility(View.GONE);
                        binding.editNameUser.setText("");
                    })
                    .addOnFailureListener(e -> { Toast.makeText(MainActivity.this, "Erro ao atualizar o usuário", Toast.LENGTH_SHORT).show(); });
        }
    }
}
