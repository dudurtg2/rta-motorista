package com.example.rta_app.SOLID.activitys;

import static androidx.fragment.app.FragmentManager.TAG;
import static com.example.rta_app.SOLID.services.NetworkService.isNetworkConnected;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.rta_app.R;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.SOLID.api.WorkerHourRepository;
import com.example.rta_app.SOLID.aplication.WorkerAplication;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.entities.utils.PointType;
import com.example.rta_app.databinding.ActivityWorkHourBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WorkHourActivity extends AppCompatActivity {

    private ActivityWorkHourBinding binding;
    private WorkerHourRepository workerHourRepository;
    private UsersRepository usersRepository;
    private WorkerAplication workerAplication;
    private FusedLocationProviderClient fusedLocationClient;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat FULL_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());

    private boolean isValidate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkHourBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);

        initDependencies();
        loadUser();
        setupClickListeners();
    }

    private void initDependencies() {
        workerHourRepository = new WorkerHourRepository(this);
        usersRepository = new UsersRepository(this);
        workerAplication = new WorkerAplication(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadInitialData();
    }

    private void loadUser() {
        usersRepository.getUser()
                .addOnSuccessListener(users -> binding.UserNameDisplay.setText(users.getName()))
                .addOnFailureListener(e -> showToast("Erro ao obter usuário: " + e.getMessage()));
    }

    private void setupClickListeners() {
        binding.progressBar.setOnClickListener(v -> showConfirmationDialog(
                "Alerta",
                "Seus pontos estão sendo registrados",
                this::updateToSheets
        ));

        binding.imageFistHour.setOnClickListener(v -> processPointClick(PointType.ENTRADA));
        binding.imageDinnerStarHour.setOnClickListener(v -> processPointClick(PointType.ALMOCO_INICIO));
        binding.imageDinnerFinishHour.setOnClickListener(v -> processPointClick(PointType.ALMOCO_FIM));
        binding.imageStop.setOnClickListener(v -> processPointClick(PointType.FIM));
    }

    private void processPointClick(PointType type) {
        workerHourRepository.getWorkerHous().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                showToast("Erro ao carregar ponto atual");
                return;
            }

            WorkerHous workerHous = task.getResult();
            if (workerHous != null && isBlank(workerHous.getHour_stop())) {
                validateAndOpenPoint(type, workerHous);
            } else {
                updateToSheets();
            }
        });
    }

    private void validateAndOpenPoint(PointType type, WorkerHous workerHous) {
        if (!isBlank(workerHous.getHour_after()) && !canRegisterNewPoint(workerHous.getDate(), workerHous.getHour_after())) {
            showAlert("Alerta", "Você não pode registrar mais de \n15 minutos após o horário anterior");
            return;
        }

        if (!isValidate) {
            showAlert("Alerta", "Você já bateu os pontos\n\nDescanse e se prepare para o trabalho", this::finish);
            return;
        }

        if (type == PointType.FIM && !isNetworkConnected(this)) {
            showAlert("Alerta", "Para registrar a saída, você precisa estar conectado à internet", this::finish);
            return;
        }

        boolean canRegister = false;
        switch (type) {
            case ENTRADA:
                canRegister = isBlank(workerHous.getHour_first());
                break;
            case ALMOCO_INICIO:
                canRegister = isBlank(workerHous.getHour_dinner());
                break;
            case ALMOCO_FIM:
                canRegister = isBlank(workerHous.getHour_finish());
                break;
            case FIM:
                canRegister = isBlank(workerHous.getHour_stop());
                break;
        }

        if (canRegister) {
            confirmPointRegistration(type);
        }
    }

    private void confirmPointRegistration(PointType type) {
        Date now = new Date();
        String message = "Deseja registrar o horário de " + type.getLabel() + "?\nDia: "
                + DATE_FORMAT.format(now) + " às " + TIME_FORMAT.format(now);

        new AlertDialog.Builder(this)
                .setTitle("Confirmar registro")
                .setMessage(message)
                .setPositiveButton("Sim", (dialog, which) -> savePoint(type))
                .setNegativeButton("Não", null)
                .show();
    }

    private void checkVehicleInspection(boolean isInitialCheck) {
        workerHourRepository.getWorkerHous().addOnSuccessListener(workerHous -> {
            boolean isVerified = isInitialCheck ? workerHous.getCarroInicial() : workerHous.getCarroFinal();

            if (!isVerified) {
                String msg = isInitialCheck ? "faça a verificação inicial" : "Para registrar a saída, faça a verificação final.";

                new AlertDialog.Builder(this)
                        .setTitle("Alerta")
                        .setMessage(msg)
                        .setNeutralButton("Ok, Entendi", (dialog, which) -> {
                            startActivity(new Intent(this, CarroRotasActivity.class));
                            finish();
                        }).show();
            } else {
                if (!isInitialCheck) {
                    processPointClick(PointType.FIM);
                }
            }
        });
    }

    private void savePoint(PointType type) {
        getCurrentPointLocation(location ->
                updateWorkerHoursData(type, location)
                        .addOnSuccessListener(aVoid -> workerHourRepository.getWorkerHous()
                                .addOnSuccessListener(workerHous -> {
                                    if (type == PointType.FIM) {
                                        updateToSheets();
                                    }
                                    updateButtonUI(type, workerHous);
                                    loadInitialData();
                                })
                        )
                        .addOnFailureListener(e -> showToast("Erro ao salvar ponto: " + e.getMessage()))
        );
    }

    private void updateToSheets() {
        isValidate = false;
        if (!isNetworkConnected(this)) {
            showToast("Você não está conectado a internet");
            return;
        }

        try {
            binding.progressBar.setVisibility(View.VISIBLE);
            workerAplication.Finish(binding.UserNameDisplay.getText().toString())
                    .addOnSuccessListener(v -> {
                        binding.progressBar.setVisibility(View.GONE);
                        showToast("Horário registrado com sucesso!");
                        finish();
                    })
                    .addOnFailureListener(v -> {
                        binding.progressBar.setVisibility(View.GONE);
                        showToast("Registro de hora falhou!");
                    });
        } catch (Exception e) {
            binding.progressBar.setVisibility(View.GONE);
            showToast("Erro ao enviar dados: " + e.getMessage());
        }
    }

    public boolean canRegisterNewPoint(String previousDate, String previousHour) {
        try {
            String previousDateTimeStr = previousDate + " " + previousHour;
            Date previousDateTime = FULL_DATE_FORMAT.parse(previousDateTimeStr);
            Date now = new Date();

            if (previousDateTime == null) {
                return true;
            }

            long diffInMillis = now.getTime() - previousDateTime.getTime();
            long diffInMinutes = diffInMillis / (1000 * 60);

            return diffInMinutes >= 2;
        } catch (ParseException e) {
            e.printStackTrace();
            return true;
        }
    }

    private Task<Void> updateWorkerHoursData(PointType type, Location location) {
        String currentTime = TIME_FORMAT.format(new Date());
        String currentDate = DATE_FORMAT.format(new Date());

        final String latitude = location != null ? String.valueOf(location.getLatitude()) : "";
        final String longitude = location != null ? String.valueOf(location.getLongitude()) : "";

        return workerHourRepository.getWorkerHous().continueWithTask(task -> {
            WorkerHous workerHous = task.getResult();

            if (workerHous == null || isBlank(workerHous.getDate())) {
                workerHous = new WorkerHous(
                        currentDate,
                        "",
                        "",
                        "",
                        "",
                        currentTime,
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""
                );
            }


            switch (type) {
                case ENTRADA:
                    workerHous.setHour_first(currentTime);
                    workerHous.setLatitude_first(latitude);
                    workerHous.setLongitude_first(longitude);
                    break;

                case ALMOCO_INICIO:
                    workerHous.setHour_dinner(currentTime);
                    workerHous.setLatitude_dinner(latitude);
                    workerHous.setLongitude_dinner(longitude);
                    break;

                case ALMOCO_FIM:
                    workerHous.setHour_finish(currentTime);
                    workerHous.setLatitude_finish(latitude);
                    workerHous.setLongitude_finish(longitude);
                    break;

                case FIM:
                    workerHous.setHour_stop(currentTime);
                    workerHous.setLatitude_stop(latitude);
                    workerHous.setLongitude_stop(longitude);
                    break;
            }

            workerHous.setHour_after(currentTime);
            return workerHourRepository.saveWorkerHous(workerHous);
        });
    }

    private void updateButtonUI(PointType type, WorkerHous workerHous) {
        if (workerHous == null) {
            return;
        }

        switch (type) {
            case ENTRADA:
                binding.buttonFistHour.setText(workerHous.getHour_first());
                break;
            case ALMOCO_INICIO:
                binding.buttonDinnerStarHour.setText(workerHous.getHour_dinner());
                break;
            case ALMOCO_FIM:
                binding.buttonDinnerFinishHour.setText(workerHous.getHour_finish());
                break;
            case FIM:
                binding.buttonStop.setText(workerHous.getHour_stop());
                break;
        }
    }

    private void loadInitialData() {
        workerHourRepository.getWorkerHous().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WorkerHous workerHous = task.getResult();
                Log.i("TESTEEEEE", "workerHous=" + workerHous.toString());
                if (workerHous != null) {
                    updateFullUI(workerHous);
                } else {
                    resetUI();
                }
            } else {
                showToast("Falha ao carregar dados");
            }
        });
    }

    private void updateFullUI(WorkerHous wh) {
        updateStatusUI(wh.getHour_first(), binding.buttonFistHour, binding.imageFistHour,
                binding.buttonDinnerStarHour, binding.imageDinnerStarHour, "Entrada");

        updateStatusUI(wh.getHour_dinner(), binding.buttonDinnerStarHour, binding.imageDinnerStarHour,
                binding.buttonDinnerFinishHour, binding.imageDinnerFinishHour, "Almoço");

        updateStatusUI(wh.getHour_finish(), binding.buttonDinnerFinishHour, binding.imageDinnerFinishHour,
                binding.buttonStop, binding.imageStop, "Saída");

        if (!isBlank(wh.getHour_stop())) {
            binding.buttonStop.setText(wh.getHour_stop());
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonStop.setText("Fim do Expediente");
        }
    }

    private void updateStatusUI(String hourValue, TextView btn, ImageView img,
                                View nextBtn, View nextImg, String defaultText) {
        boolean temHorario = !isBlank(hourValue);

        if (temHorario) {
            btn.setText(hourValue);
            img.setImageResource(R.drawable.confirm_hour);
            img.setBackgroundResource(R.drawable.button_stroke_squad);

            btn.setVisibility(View.VISIBLE);
            img.setVisibility(View.VISIBLE);

            if (nextBtn != null) {
                nextBtn.setVisibility(View.VISIBLE);
                nextBtn.bringToFront();
            }
            if (nextImg != null) {
                nextImg.setVisibility(View.VISIBLE);
                nextImg.bringToFront();
            }
        } else {
            btn.setText(defaultText);
            if (nextBtn != null) {
                nextBtn.setVisibility(View.GONE);
            }
            if (nextImg != null) {
                nextImg.setVisibility(View.GONE);
            }
        }
    }

    private void resetUI() {
        binding.buttonStop.setText("Fim");
        binding.buttonDinnerFinishHour.setText("Saída");
        binding.buttonDinnerStarHour.setText("Almoço");
        binding.buttonFistHour.setText("Entrada");
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showAlert(String title, String msg) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setNeutralButton("Ok", null)
                .show();
    }

    private void showAlert(String title, String msg, Runnable onOk) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setNeutralButton("Ok", (d, w) -> onOk.run())
                .show();
    }

    private void showConfirmationDialog(String title, String msg, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setNeutralButton("Ok", (d, w) -> onConfirm.run())
                .show();
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void getCurrentPointLocation(PointLocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onLocationResult(null);
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocationResult(location);
                    } else {
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(callback::onLocationResult)
                                .addOnFailureListener(e -> callback.onLocationResult(null));
                    }
                })
                .addOnFailureListener(e -> fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(callback::onLocationResult)
                        .addOnFailureListener(err -> callback.onLocationResult(null)));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private interface PointLocationCallback {
        void onLocationResult(Location location);
    }
}
