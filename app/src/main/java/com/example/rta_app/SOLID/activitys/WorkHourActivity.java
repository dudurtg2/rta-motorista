package com.example.rta_app.SOLID.activitys;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.rta_app.SOLID.services.UserDAO;
import com.example.rta_app.R;
import com.example.rta_app.SOLID.repository.RTArepository;
import com.example.rta_app.databinding.ActivityWorkHourBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WorkHourActivity extends AppCompatActivity {
    private DocumentReference docRef, docHour;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    public static final int REQUEST_IMAGE_CAPTURE = 2;
    public ActivityWorkHourBinding binding;
    private String hourType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_hour);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        EdgeToEdge.enable(this);

        if (mAuth.getCurrentUser() != null) {
            docRef = firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid());
            docHour = firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid()).collection("work_hours").document(new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
        } else {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding = ActivityWorkHourBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
        new RTArepository(this).getUserName(userName ->
                binding.UserNameDisplay.setText(userName)
        );
    }

    @Override
    protected void onStart() {
        super.onStart();

        loadInitialData();
    }



    private void validateFields(String hour, OnValidationCompleteListener listener) {
        docHour = firestore.collection("usuarios")
                .document(mAuth.getCurrentUser().getUid())
                .collection("work_hours")
                .document("cachehoras");

        docHour.get().addOnSuccessListener(documentSnapshot -> {
            boolean isFieldValid = false;
            boolean isPreviousFieldValid = true;

            if (documentSnapshot.exists()) {

                isFieldValid = !documentSnapshot.contains(hour);

                switch (hour) {
                    case "Almoço":
                        isPreviousFieldValid = documentSnapshot.contains("Entrada");
                        break;
                    case "Saída":
                        isPreviousFieldValid = documentSnapshot.contains("Almoço");
                        break;
                    case "Fim":
                        isPreviousFieldValid = documentSnapshot.contains("Saída");
                        break;
                    default:
                        isPreviousFieldValid = true;
                }
            } else {
                isFieldValid = hour.equals("Entrada");
            }
            listener.onComplete(isFieldValid && isPreviousFieldValid);
        }).addOnFailureListener(e -> {
            listener.onComplete(false);
        });
    }



    interface OnValidationCompleteListener {
        void onComplete(boolean isValid);
    }

    private void setupClickListeners() {
        binding.imageFistHour.setOnClickListener(v -> validateFields("Entrada", isValid -> {
            if (isValid) {

                hourType = "Entrada";
                openPonts(hourType);
            } else {
                Toast.makeText(this, "O horário de entrada já foi registrado.", Toast.LENGTH_SHORT).show();
            }
        }));

        binding.imageDinnerStarHour.setOnClickListener(v -> validateFields("Almoço", isValid -> {
            if (isValid) {

                hourType = "Almoço";
                openPonts(hourType);
            } else {
                Toast.makeText(this, "O horário de Almoço já foi registrado ou o ponto de Entrada não foi registrado.", Toast.LENGTH_SHORT).show();
            }
        }));

        binding.imageDinnerFinishHour.setOnClickListener(v -> validateFields("Saída", isValid -> {
            if (isValid) {

                hourType = "Saída";
                openPonts(hourType);
            } else {
                Toast.makeText(this, "O horário de Saída já foi registrado ou o ponto de Almoço não foi registrado.", Toast.LENGTH_SHORT).show();
            }
        }));

        binding.imageStop.setOnClickListener(v -> validateFields("Fim", isValid -> {
            if (isValid) {

                hourType = "Fim";
                openPonts(hourType);
            } else {
                Toast.makeText(this, "O horário de Fim já foi registrado ou o ponto de Saída não foi registrado.", Toast.LENGTH_SHORT).show();
            }
        }));
    }


    private void openPonts(String hour) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar registro de hora")
                .setMessage("Deseja registar o horário de " + hourType + "?" + "\nDia: " + new SimpleDateFormat("dd-MM-yyyy").format(new Date()) +" as "+ new SimpleDateFormat("HH:mm").format(new Date()))
                .setPositiveButton("Sim", (dialog, which) -> {

                    updateWorkHour(hour);

                })
                .setNegativeButton("Não", (dialog, which) -> {

                    dialog.dismiss();
                })
                .show();
    }


    private void updateWorkHour(String hourType) {
        UpdateWorkHours(hourType).addOnSuccessListener(task -> docHour.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                switch (hourType) {
                    case "Entrada":
                        binding.buttonFistHour.setText(documentSnapshot.getString("Entrada"));
                        loadInitialData();
                        break;
                    case "Almoço":
                        binding.buttonDinnerStarHour.setText(documentSnapshot.getString("Almoço"));
                        loadInitialData();
                        break;
                    case "Saída":
                        binding.buttonDinnerFinishHour.setText(documentSnapshot.getString("Saída"));
                        loadInitialData();
                        break;
                    case "Fim":
                        binding.buttonStop.setText(documentSnapshot.getString("Fim"));
                        loadInitialData();
                        break;
                }
            }
        }));
    }

    private void loadInitialData() {
        docHour = firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid()).collection("work_hours").document("cachehoras");
        docHour.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("Entrada")) {
                    binding.buttonFistHour.setText(documentSnapshot.getString("Entrada"));
                    binding.imageFistHour.setImageResource(R.drawable.confirm_hour);
                    binding.imageDinnerStarHour.setVisibility(View.VISIBLE);
                    binding.buttonDinnerStarHour.setVisibility(View.VISIBLE);
                    binding.imageDinnerStarHour.setImageResource(R.drawable.clock_hour);

                } else {
                    binding.buttonFistHour.setText("Entrada");
                }
                if (documentSnapshot.contains("Almoço")) {
                    binding.buttonDinnerStarHour.setText(documentSnapshot.getString("Almoço"));
                    binding.imageDinnerStarHour.setImageResource(R.drawable.confirm_hour);

                    binding.imageDinnerFinishHour.setVisibility(View.VISIBLE);
                    binding.buttonDinnerFinishHour.setVisibility(View.VISIBLE);
                    binding.imageDinnerFinishHour.setImageResource(R.drawable.clock_hour);
                } else {
                    binding.buttonDinnerStarHour.setText("Almoço");
                }
                if (documentSnapshot.contains("Saída")) {
                    binding.buttonDinnerFinishHour.setText(documentSnapshot.getString("Saída"));
                    binding.imageDinnerFinishHour.setImageResource(R.drawable.confirm_hour);

                    binding.imageStop.setVisibility(View.VISIBLE);
                    binding.buttonStop.setVisibility(View.VISIBLE);
                    binding.imageStop.setImageResource(R.drawable.clock_hour);
                } else {
                    binding.buttonDinnerFinishHour.setText("Saída");
                }
                if (documentSnapshot.contains("Fim")) {
                    binding.buttonStop.setText(documentSnapshot.getString("Fim"));
                    binding.imageStop.setImageResource(R.drawable.confirm_hour);
                } else {
                    binding.buttonStop.setText("Fim");
                }

            } else {
                binding.buttonStop.setText("Fim");
                binding.buttonDinnerFinishHour.setText("Saída");
                binding.buttonDinnerStarHour.setText("Almoço");
                binding.buttonFistHour.setText("Entrada");
            }
        });
    }


    private Task<Void> UpdateWorkHours(String horario) {
        String timeString = new SimpleDateFormat("HH:mm").format(new Date());

        return new UserDAO(this).updateWorkHours(binding.UserNameDisplay.getText().toString(),mAuth.getCurrentUser().getUid(), "cachehoras", timeString, horario);
    }
}
