package com.example.rta_app.SOLID.activitys;

import static com.example.rta_app.SOLID.services.NetworkService.isNetworkConnected;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.rta_app.R;
import com.example.rta_app.SOLID.aplication.WorkerAplication;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.SOLID.api.WorkerHourRepository;
import com.example.rta_app.databinding.ActivityWorkHourBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WorkHourActivity extends AppCompatActivity {

    public ActivityWorkHourBinding binding;
    private WorkerHourRepository workerHourRepository;
    private UsersRepository usersRepository;

    private WorkerAplication workerAplication;

    private Boolean isValidate = true;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkHourBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);
        workerHourRepository = new WorkerHourRepository(this);
        usersRepository = new UsersRepository(this);
        workerAplication = new WorkerAplication(this);
        getUser();
        setupClickListeners();
        locAtive();
    }


    private void locAtive() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();

                        if (isInLocation(lat, lng)) {
                            binding.imageFistHour.setOnClickListener(v -> openPontsIsFinish("Entrada"));
                        } else {
                            if( binding.imageDinnerFinishHour.getVisibility() == View.VISIBLE) return;
                            binding.imageFistHour.setOnClickListener(v -> {
                                if(!binding.buttonFistHour.getText().toString().equals("Entrada")) return;
                                EditText input = new EditText(this);
                                input.setHint("Código de liberação");
                                LinearLayout container = new LinearLayout(this);
                                container.setPadding(50, 0, 50, 0);
                                container.addView(input);

                                new AlertDialog.Builder(this)
                                        .setTitle("Alerta")
                                        .setMessage("Você não pode registrar o ponto\nfora do local da base.\n\nSolicite um código de liberação:")
                                        .setView(container)
                                        .setPositiveButton("Enviar", (dialog, which) -> {
                                            workerHourRepository.validadeCode(input.getText().toString().toUpperCase())
                                                    .addOnSuccessListener(d -> openPontsIsFinish("Entrada"))
                                                    .addOnFailureListener(c ->
                                                            new AlertDialog.Builder(this)
                                                            .setTitle("Alerta")
                                                            .setMessage("Código de validação inválido")
                                                            .setNeutralButton("Ok", (dialog2, which2) -> finish()).show()
                                            );
                                        })
                                        .setNeutralButton("Cancelar", (dialog, which) -> dialog.dismiss())
                                        .show();
                            });
                        }
                    }
                });


    }

    private boolean isInLocation(double lat, double lng) {
        /*double destinoLat;
        double destinoLng;

        switch (usersRepository.getUser().getResult().getBaseid()){
            case 1:
                destinoLat = -12.255348493385583;
                destinoLng = -38.92503847319095;
                break;
            default:
                return true;
        }
        float[] resultado = new float[1];

        Location.distanceBetween(lat, lng, destinoLat, destinoLng, resultado);

       *//* return resultado[0] < 300;*/
        return true;
    }

    private void getUser() {
        usersRepository.getUser()
                .addOnSuccessListener(users -> {
                    binding.UserNameDisplay.setText(users.getName());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao obter usuário: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadInitialData();

    }

    private void setupClickListeners() {
        binding.progressBar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Alerta")
                    .setMessage("Seus pontos estão sendo \nregistrados")
                    .setNeutralButton("ok", (dialog, which) -> updateToSheets()).show();
        });

        binding.imageDinnerStarHour.setOnClickListener(v -> openPontsIsFinish("Almoço"));
        binding.imageDinnerFinishHour.setOnClickListener(v -> openPontsIsFinish("Saída"));
        binding.imageStop.setOnClickListener(v -> openPontsIsFinish("Fim"));
    }

    private void alert() {
        new AlertDialog.Builder(this)
                .setTitle("Alerta")
                .setMessage("Você ja bateu os pontos\n\nDescanse e se prepare \npara o trabalho")
                .setNeutralButton("Descansa", (dialog, which) -> finish()).show();

    }

    private void openPontsIsFinish(String valueForHourUpdate) {
        workerHourRepository.getWorkerHous().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WorkerHous workerHous = task.getResult();
                if (workerHous.getHour_stop().equals("")) {
                    openPontsIsValidade(valueForHourUpdate, workerHous);
                } else {
                    updateToSheets();
                }
            }
        });
    }

    private void openPontsIsValidade(String valueForHourUpdate, WorkerHous workerHous) {
        if (!workerHous.getHour_after().equals("") && !isAfter20Minutes(workerHous.getDate(), workerHous.getHour_after())) {
            new AlertDialog.Builder(this)
                    .setTitle("Alerta")
                    .setMessage("Você não pode registrar mais de \n15 minutos após o horário anterior")
                    .setNeutralButton("Ok", (dialog, which) -> dialog.dismiss()).show();
        } else if (!isValidate) {
            alert();
        } else {
            switch (valueForHourUpdate) {
                case "Entrada":
                    if (workerHous.getHour_first().isEmpty()) {
                        openPonts(valueForHourUpdate);
                    }
                    break;
                case "Almoço":
                    if (workerHous.getHour_dinner().isEmpty()) {
                        openPonts(valueForHourUpdate);
                    }
                    break;
                case "Saída":
                    if (workerHous.getHour_finish().isEmpty()) {
                        openPonts(valueForHourUpdate);
                    }
                    break;
                case "Fim":
                    if (!isNetworkConnected(this)) {
                        new AlertDialog.Builder(this)
                                .setTitle("Alerta")
                                .setMessage("Para registrar o ponto de saida,\nVocê precisa estar conectado\na internet")
                                .setNeutralButton("Ok, Entendi", (dialog, which) -> finish()).show();
                    } else if (workerHous.getHour_stop().isEmpty()) {
                        openPonts(valueForHourUpdate);
                    }
            }
        }
    }

    private void openPonts(String valueForHourUpdate) {

        if (valueForHourUpdate == "Fim" && !isNetworkConnected(this)) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Confirmar registro de hora")
                .setMessage("Deseja registrar o horário de " + valueForHourUpdate + "?\nDia: "
                        + new SimpleDateFormat("dd-MM-yyyy").format(new Date())
                        + " às " + new SimpleDateFormat("HH:mm").format(new Date()))
                .setPositiveButton("Sim", (dialog, which) -> updateHours(valueForHourUpdate))
                .setNegativeButton("Não", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateHours(String valueForHourUpdate) {
        UpdateWorkHours(valueForHourUpdate).addOnSuccessListener(aVoid -> {
            workerHourRepository.getWorkerHous().addOnSuccessListener(updatedTask -> {
                WorkerHous updatedWorkerHous = updatedTask;
                if (valueForHourUpdate.equals("Fim")) {
                    updateToSheets();
                }
                updateButtonText(valueForHourUpdate, updatedWorkerHous);
                loadInitialData();
            });
        });
    }

    private void updateToSheets() {
        isValidate = false;
        try {
            if (isNetworkConnected(this)) {
                workerAplication.Finish(binding.UserNameDisplay.getText().toString()).addOnSuccessListener(v -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonFistHour.setVisibility(View.VISIBLE);
                    binding.imageFistHour.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Horário registrado com sucesso!", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(v -> Toast.makeText(this, "Registro de hora falhou!", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Você não está conectado a internet", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {

        }
    }

    private void updateButtonText(String valueForHourUpdate, WorkerHous workerHous) {
        if (workerHous != null) {
            switch (valueForHourUpdate) {
                case "Entrada":
                    binding.buttonFistHour.setText(workerHous.getHour_first());
                    break;
                case "Almoço":
                    binding.buttonDinnerStarHour.setText(workerHous.getHour_dinner());
                    break;
                case "Saída":
                    binding.buttonDinnerFinishHour.setText(workerHous.getHour_finish());
                    break;
                case "Fim":
                    binding.buttonStop.setText(workerHous.getHour_stop());
                    break;
                default:
                    Toast.makeText(this, "Tipo de hora desconhecido: " + valueForHourUpdate, Toast.LENGTH_SHORT).show();
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
            binding.imageFistHour.setBackgroundResource(R.drawable.button_stroke_squad);

            binding.imageDinnerStarHour.setVisibility(View.VISIBLE);
            binding.buttonDinnerStarHour.setVisibility(View.VISIBLE);
            binding.imageDinnerStarHour.setImageResource(R.drawable.clock_hour);
        } else {
            binding.buttonFistHour.setText("Entrada");
        }

        if (!workerHous.getHour_dinner().isEmpty()) {
            binding.buttonDinnerStarHour.setText(workerHous.getHour_dinner());
            binding.imageDinnerStarHour.setImageResource(R.drawable.confirm_hour);
            binding.imageDinnerStarHour.setBackgroundResource(R.drawable.button_stroke_squad);

            binding.imageDinnerFinishHour.setVisibility(View.VISIBLE);
            binding.buttonDinnerFinishHour.setVisibility(View.VISIBLE);
            binding.imageDinnerFinishHour.setImageResource(R.drawable.clock_hour);
        } else {
            binding.buttonDinnerStarHour.setText("Almoço");
        }

        if (!workerHous.getHour_finish().isEmpty()) {
            binding.buttonDinnerFinishHour.setText(workerHous.getHour_finish());
            binding.imageDinnerFinishHour.setImageResource(R.drawable.confirm_hour);
            binding.imageDinnerFinishHour.setBackgroundResource(R.drawable.button_stroke_squad);

            binding.imageStop.setVisibility(View.VISIBLE);
            binding.buttonStop.setVisibility(View.VISIBLE);
            binding.imageStop.setImageResource(R.drawable.clock_hour);
        } else {
            binding.buttonDinnerFinishHour.setText("Saída");
        }

        if (!workerHous.getHour_stop().isEmpty()) {
            binding.buttonDinnerStarHour.setVisibility(View.GONE);
            binding.buttonDinnerFinishHour.setVisibility(View.GONE);
            binding.buttonStop.setVisibility(View.GONE);
            binding.imageDinnerStarHour.setVisibility(View.GONE);
            binding.imageDinnerFinishHour.setVisibility(View.GONE);
            binding.imageStop.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonFistHour.setText("Descansa");
            binding.buttonFistHour.setVisibility(View.GONE);
            binding.imageFistHour.setVisibility(View.GONE);
        } else {
            binding.buttonStop.setText("Fim do Expediente");
        }
    }

    private void resetUI() {
        binding.buttonStop.setText("Fim");
        binding.buttonDinnerFinishHour.setText("Saída");
        binding.buttonDinnerStarHour.setText("Almoço");
        binding.buttonFistHour.setText("Entrada");
    }

    public boolean isAfter20Minutes(String previousDate, String previousHour) {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        try {

            String previousDateTime = previousDate + " " + previousHour;

            Date previousDateTimeParsed = format.parse(previousDateTime);
            Date currentDate = new Date();

            long diffInMinutes = (currentDate.getTime() - previousDateTimeParsed.getTime()) / (1000 * 60);

            return diffInMinutes >= 15;


        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Task<Void> UpdateWorkHours(String registroDePonto) {
        String timeString = new SimpleDateFormat("HH:mm").format(new Date());
        String data = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

        return workerHourRepository.getWorkerHous()
                .continueWithTask(task -> {
                    WorkerHous workerHous = task.getResult();

                    if (workerHous == null || workerHous.getDate().isEmpty()) {
                        workerHous = new WorkerHous(data, "", "", "", "", timeString);
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
                    workerHous.setHour_after(timeString);

                    return workerHourRepository.saveWorkerHous(workerHous);
                });
    }
}
