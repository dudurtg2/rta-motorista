package com.example.rta_app.SOLID.activitys;

import static com.example.rta_app.SOLID.services.NetworkService.isNetworkConnected;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rta_app.R;
import com.example.rta_app.SOLID.Interfaces.IUsersRepository;
import com.example.rta_app.SOLID.Interfaces.IWorkerHourRepository;
import com.example.rta_app.SOLID.aplication.WorkerAplication;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.repository.UsersRepository;
import com.example.rta_app.SOLID.repository.WorkerHourRepository;
import com.example.rta_app.SOLID.services.GoogleSheetsService;
import com.example.rta_app.SOLID.services.NetworkService;
import com.example.rta_app.databinding.ActivityWorkHourBinding;
import com.google.android.gms.tasks.Task;

import org.checkerframework.checker.units.qual.N;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WorkHourActivity extends AppCompatActivity {

    public ActivityWorkHourBinding binding;
    private IWorkerHourRepository workerHourRepository;
    private IUsersRepository usersRepository;

    private WorkerAplication workerAplication;

    private Boolean isValidate = true;

    public WorkHourActivity() {
        workerHourRepository = new WorkerHourRepository(this);
        usersRepository = new UsersRepository();
        workerAplication = new WorkerAplication(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkHourBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);

        getUser();
        setupClickListeners();

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
                .setNeutralButton("ok", (dialog, which) -> closeOptionsMenu()).show();
        });
       binding.imageFistHour.setOnClickListener(v -> openPontsIsFinish("Entrada"));
       binding.imageDinnerStarHour.setOnClickListener(v -> openPontsIsFinish("Almoço"));
       binding.imageDinnerFinishHour.setOnClickListener(v -> openPontsIsFinish("Saída"));
       binding.imageStop.setOnClickListener(v -> openPontsIsFinish("Fim"));
    }

    private void alert(){
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
                    .setMessage("Você não pode registrar mais de \n20 minutos após o horário anterior")
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
                    if(!isNetworkConnected(this)){
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

        if(valueForHourUpdate == "Fim" && !isNetworkConnected(this)) {
            return;
        }  new AlertDialog.Builder(this)
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
            if(isNetworkConnected(this)){
                workerAplication.Finish(binding.UserNameDisplay.getText().toString()).addOnSuccessListener(v -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonFistHour.setVisibility(View.VISIBLE);
                    binding.imageFistHour.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Horário registrado com sucesso!", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(v -> Toast.makeText(this, "Registro de hora falhou!", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Você não está conectado a internet", Toast.LENGTH_SHORT).show();
           }
        } catch (Exception e){

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
        }else {
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

            return diffInMinutes >= 20;
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
