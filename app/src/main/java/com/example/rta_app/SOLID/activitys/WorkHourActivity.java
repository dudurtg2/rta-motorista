package com.example.rta_app.SOLID.activitys;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rta_app.R;
import com.example.rta_app.SOLID.repository.RTArepository;
import com.example.rta_app.databinding.ActivityWorkHourBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WorkHourActivity extends AppCompatActivity {
    private DocumentReference  docHour;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    public ActivityWorkHourBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_hour);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        EdgeToEdge.enable(this);

        if (mAuth.getCurrentUser() != null) {
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
                openPonts("Entrada","null");
            }
        }));
        binding.imageDinnerStarHour.setOnClickListener(v -> validateFields("Almoço", isValid -> {
            if (isValid) {
                openPonts("Almoço" , "Entrada");
            }
        }));
        binding.imageDinnerFinishHour.setOnClickListener(v -> validateFields("Saída", isValid -> {
            if (isValid) {
                openPonts("Saída", "Almoço");
            }
        }));

        binding.imageStop.setOnClickListener(v -> validateFields("Fim", isValid -> {
            if (isValid) {
                openPonts("Fim", "Saída");
            }
        }));
    }


    private void openPonts(String hour,String hourAfter) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar registro de hora")
                .setMessage("Deseja registar o horário de " + hour + "?" + "\nDia: " + new SimpleDateFormat("dd-MM-yyyy").format(new Date()) +" as "+ new SimpleDateFormat("HH:mm").format(new Date()))
                .setPositiveButton("Sim", (dialog, which) -> updateWorkHour(hour, hourAfter))
                .setNegativeButton("Não", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void updateWorkHour(String hourType, String hourTypeAfter) {
        if (!hourTypeAfter.equals("null")) {
        docHour.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains(hourTypeAfter)) {
                        String lastRecordedTimeString = documentSnapshot.getString(hourTypeAfter);
                        if (lastRecordedTimeString != null) {
                            SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            try {
                                Date currentTime = format.parse(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
                                Date lastRecordedTime = format.parse(lastRecordedTimeString);
                                long diff = currentTime.getTime() - lastRecordedTime.getTime();
                                long diffMinutes = diff / (60 * 1000);

                                if (diffMinutes < 30) {
                                    int remainingMinutes = 30 - (int) diffMinutes;
                                    Toast.makeText(this, "Ainda falta " + remainingMinutes + " minutos para o próxima ponto", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            } catch (ParseException e) {
                                Toast.makeText(this, "Erro ao analisar o horário: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }

                horasUpdate( hourType,hourTypeAfter);

            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Falha ao recuperar dados atuais: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
        } else {
            horasUpdate( hourType,hourTypeAfter);
        }
    }

    private void horasUpdate(String hourType, String hourTypeAfter){
        UpdateWorkHours(hourType, hourTypeAfter).addOnSuccessListener(aVoid -> {
            docHour.get().addOnSuccessListener(updatedDocumentSnapshot -> {
                if (updatedDocumentSnapshot.exists()) {
                    switch (hourType) {
                        case "Entrada":
                            binding.buttonFistHour.setText(updatedDocumentSnapshot.getString("Entrada"));
                            break;
                        case "Almoço":
                            binding.buttonDinnerStarHour.setText(updatedDocumentSnapshot.getString("Almoço"));
                            break;
                        case "Saída":
                            binding.buttonDinnerFinishHour.setText(updatedDocumentSnapshot.getString("Saída"));
                            break;
                        case "Fim":
                            binding.buttonStop.setText(updatedDocumentSnapshot.getString("Fim"));
                            break;
                    }
                    loadInitialData();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Falha ao recuperar dados atualizados: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Falha ao atualizar horário: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
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


    private Task<Void> UpdateWorkHours(String restistoDePonto, String resgistoAnterior) {
        String timeString = new SimpleDateFormat("HH:mm").format(new Date());



        return new RTArepository(this).updateWorkHours(binding.UserNameDisplay.getText().toString(),mAuth.getCurrentUser().getUid(), "cachehoras", timeString, restistoDePonto);
    }
}
