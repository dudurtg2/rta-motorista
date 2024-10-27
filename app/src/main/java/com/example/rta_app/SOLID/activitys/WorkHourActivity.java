package com.example.rta_app.SOLID.activitys;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rta_app.R;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.repository.RTArepository;
import com.example.rta_app.SOLID.repository.WorkerHourRepository;
import com.example.rta_app.SOLID.services.GoogleSheetsService;
import com.example.rta_app.databinding.ActivityWorkHourBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WorkHourActivity extends AppCompatActivity {
    private DocumentReference docHour;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    public ActivityWorkHourBinding binding;
    private WorkerHourRepository workerHourRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkHourBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        workerHourRepository = new WorkerHourRepository();
        EdgeToEdge.enable(this);

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        docHour = firestore.collection("usuarios")
                .document(userId)
                .collection("work_hours")
                .document("cachehoras");

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

    private void setupClickListeners() {
        binding.imageFistHour.setOnClickListener(v -> openPonts("Entrada"));
        binding.imageDinnerStarHour.setOnClickListener(v -> openPonts("Almoço"));
        binding.imageDinnerFinishHour.setOnClickListener(v -> openPonts("Saída"));
        binding.imageStop.setOnClickListener(v -> openPonts("Fim"));
    }

    private void openPonts(String hour) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar registro de hora")
                .setMessage("Deseja registrar o horário de " + hour + "?\nDia: "
                        + new SimpleDateFormat("dd-MM-yyyy").format(new Date())
                        + " às " + new SimpleDateFormat("HH:mm").format(new Date()))
                .setPositiveButton("Sim", (dialog, which) -> horasUpdate(hour))
                .setNegativeButton("Não", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void horasUpdate(String hourType) {
        if (docHour == null) {
            Toast.makeText(this, "Documento de horas não inicializado.", Toast.LENGTH_SHORT).show();
            return;
        }
        UpdateWorkHours(hourType)
                .addOnSuccessListener(aVoid -> {
                    docHour.get().addOnSuccessListener(updatedDocumentSnapshot -> {
                        if (updatedDocumentSnapshot.exists()) {
                            updateButtonText(hourType, updatedDocumentSnapshot);
                            loadInitialData();
                            if (hourType == "Fim") {
                                workerHourRepository.getWorkerHous()
                                        .continueWithTask(task -> {
                                            WorkerHous workerHous = task.getResult();
                                            new GoogleSheetsService(this).getGoogleSheet(binding.UserNameDisplay.getText().toString(), workerHous);
                                            return null;
                                        });
                            }
                        }
                    }).addOnFailureListener(e ->
                            Toast.makeText(this, "Falha ao recuperar dados atualizados: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Falha ao atualizar horário: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateButtonText(String hourType, DocumentSnapshot updatedDocumentSnapshot) {
        String hourValue = updatedDocumentSnapshot.getString(hourType);

        if (hourValue != null) {
            switch (hourType) {
                case "Entrada":
                    binding.buttonFistHour.setText(hourValue);
                    break;
                case "Almoço":
                    binding.buttonDinnerStarHour.setText(hourValue);
                    break;
                case "Saída":
                    binding.buttonDinnerFinishHour.setText(hourValue);
                    break;
                case "Fim":
                    binding.buttonStop.setText(hourValue);
                    break;
                default:
                    Toast.makeText(this, "Tipo de hora desconhecido: " + hourType, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void loadInitialData() {
        workerHourRepository.getWorkerHous().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WorkerHous workerHous = task.getResult();
                if (workerHous != null) {
                    updateUI(workerHous);
                } else {
                    resetUI();
                }
            } else {
                Toast.makeText(this, "Falha ao carregar dados: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(WorkerHous workerHous) {
        if (!workerHous.getHour_first().isEmpty()) {
            binding.buttonFistHour.setText(workerHous.getHour_first());
            binding.imageFistHour.setImageResource(R.drawable.confirm_hour);

            binding.imageDinnerStarHour.setVisibility(View.VISIBLE);
            binding.buttonDinnerStarHour.setVisibility(View.VISIBLE);
            binding.imageDinnerStarHour.setImageResource(R.drawable.clock_hour);
        } else {
            binding.buttonFistHour.setText("Entrada");
        }

        if (!workerHous.getHour_dinner().isEmpty()) {
            binding.buttonDinnerStarHour.setText(workerHous.getHour_dinner());
            binding.imageDinnerStarHour.setImageResource(R.drawable.confirm_hour);

            binding.imageDinnerFinishHour.setVisibility(View.VISIBLE);
            binding.buttonDinnerFinishHour.setVisibility(View.VISIBLE);
            binding.imageDinnerFinishHour.setImageResource(R.drawable.clock_hour);
        } else {
            binding.buttonDinnerStarHour.setText("Almoço");
        }

        if (!workerHous.getHour_finish().isEmpty()) {
            binding.buttonDinnerFinishHour.setText(workerHous.getHour_finish());
            binding.imageDinnerFinishHour.setImageResource(R.drawable.confirm_hour);

            binding.imageStop.setVisibility(View.VISIBLE);
            binding.buttonStop.setVisibility(View.VISIBLE);
            binding.imageStop.setImageResource(R.drawable.clock_hour);
        } else {
            binding.buttonDinnerFinishHour.setText("Saída");
        }

        if (!workerHous.getHour_stop().isEmpty()) {
            binding.buttonStop.setText(workerHous.getHour_stop());
            binding.imageStop.setImageResource(R.drawable.confirm_hour);
        } else {
            binding.buttonStop.setText("Fim");
        }
    }

    private void resetUI() {
        binding.buttonStop.setText("Fim");
        binding.buttonDinnerFinishHour.setText("Saída");
        binding.buttonDinnerStarHour.setText("Almoço");
        binding.buttonFistHour.setText("Entrada");
    }

    private Task<Void> UpdateWorkHours(String registroDePonto) {
        String timeString = new SimpleDateFormat("HH:mm").format(new Date());
        String data = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

        return workerHourRepository.getWorkerHous()
                .continueWithTask(task -> {
                    WorkerHous workerHous = task.getResult();

                    if (workerHous == null || workerHous.getDate().isEmpty()) {
                        workerHous = new WorkerHous(data, "", "", "", "");
                    }

                    switch (registroDePonto) {
                        case "Entrada":
                            workerHous.setHour_first(timeString);
                            break;
                        case "Almoço":
                            workerHous.setHour_dinner(timeString);
                            break;
                        case "Saída":
                            workerHous.setHour_finish(timeString);
                            break;
                        case "Fim":
                            workerHous.setHour_stop(timeString);
                            break;
                    }

                    return workerHourRepository.saveWorkerHous(workerHous);
                });
    }
}
