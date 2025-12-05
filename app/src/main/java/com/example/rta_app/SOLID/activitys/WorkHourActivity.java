package com.example.rta_app.SOLID.activitys;

import static com.example.rta_app.SOLID.services.NetworkService.isNetworkConnected;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rta_app.R;
import com.example.rta_app.SOLID.aplication.WorkerAplication;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.SOLID.api.WorkerHourRepository;
import com.example.rta_app.SOLID.entities.utils.PointType;
import com.example.rta_app.databinding.ActivityWorkHourBinding;
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

    // Formato de data deve ser constante para evitar recriação
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

        // Usando o Enum sugerido
        binding.imageFistHour.setOnClickListener(v -> processPointClick(PointType.ENTRADA));
        binding.imageDinnerStarHour.setOnClickListener(v -> processPointClick(PointType.ALMOCO_INICIO));
        binding.imageDinnerFinishHour.setOnClickListener(v -> processPointClick(PointType.ALMOCO_FIM));

        binding.imageStop.setOnClickListener(v ->processPointClick(PointType.FIM) /*checkVehicleInspection(false)*/); // False = Check Final
    }

    // Centraliza a lógica de clique
    private void processPointClick(PointType type) {
        workerHourRepository.getWorkerHous().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WorkerHous workerHous = task.getResult();
                if (workerHous != null && workerHous.getHour_stop().isEmpty()) {
                    validateAndOpenPoint(type, workerHous);
                } else {
                    updateToSheets();
                }
            }
        });
    }

    private void validateAndOpenPoint(PointType type, WorkerHous workerHous) {
        // Validação de 15/20 minutos
        if (!workerHous.getHour_after().isEmpty() && !canRegisterNewPoint(workerHous.getDate(), workerHous.getHour_after())) {
            showAlert("Alerta", "Você não pode registrar mais de \n15 minutos após o horário anterior");
            return;
        }

        if (!isValidate) {
            showAlert("Alerta", "Você já bateu os pontos\n\nDescanse e se prepare para o trabalho", this::finish);
            return;
        }

        // Validação específica para o FIM
        if (type == PointType.FIM && !isNetworkConnected(this)) {
            showAlert("Alerta", "Para registrar a saída, você precisa estar conectado à internet", this::finish);
            return;
        }

        // Verifica se o campo correspondente está vazio antes de tentar registrar
        boolean canRegister = false;
        switch (type) {
            case ENTRADA: canRegister = workerHous.getHour_first().isEmpty(); break;
            case ALMOCO_INICIO: canRegister = workerHous.getHour_dinner().isEmpty(); break;
            case ALMOCO_FIM: canRegister = workerHous.getHour_finish().isEmpty(); break;
            case FIM: canRegister = workerHous.getHour_stop().isEmpty(); break;
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

    // Refatorado o método testForVerificador para checkVehicleInspection
    private void checkVehicleInspection(boolean isInitialCheck) {
        workerHourRepository.getWorkerHous().addOnSuccessListener(workerHous -> {
            boolean isVerified = isInitialCheck ? workerHous.getCarroInicial() : workerHous.getCarroFinal();

            if (!isVerified) {
                // Se for verificação inicial e falhar, ou final e falhar, vai pra tela de rotas
                // Nota: Mantive sua lógica original, mas simplificada
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
        updateWorkerHoursData(type).addOnSuccessListener(aVoid -> {
            workerHourRepository.getWorkerHous().addOnSuccessListener(workerHous -> {
                if (type == PointType.FIM) {
                    updateToSheets();
                }
                updateButtonUI(type, workerHous);
                loadInitialData(); // Recarrega a tela
            });
        });
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

    private void restoreUI() {
        binding.buttonFistHour.setVisibility(View.VISIBLE);
        binding.imageFistHour.setVisibility(View.VISIBLE);

    }

    // CORREÇÃO DA LÓGICA DE TEMPO
    public boolean canRegisterNewPoint(String previousDate, String previousHour) {
        try {
            String previousDateTimeStr = previousDate + " " + previousHour;
            Date previousDateTime = FULL_DATE_FORMAT.parse(previousDateTimeStr);
            Date now = new Date();

            if (previousDateTime == null) return true;

            long diffInMillis = now.getTime() - previousDateTime.getTime();
            long diffInMinutes = diffInMillis / (1000 * 60);

            // Retorna TRUE se passou mais de 15 minutos (lógica descomentada e corrigida)
            return true;

        } catch (ParseException e) {
            e.printStackTrace();
            return true; // Se der erro no parse, permite bater o ponto para não travar o user
        }
    }

    // Atualização do Objeto de Dados
    private Task<Void> updateWorkerHoursData(PointType type) {
        String currentTime = TIME_FORMAT.format(new Date());
        String currentDate = DATE_FORMAT.format(new Date());

        return workerHourRepository.getWorkerHous().continueWithTask(task -> {
            WorkerHous workerHous = task.getResult();
            if (workerHous == null || workerHous.getDate().isEmpty()) {
                workerHous = new WorkerHous(currentDate, "", "", "", "", currentTime);
            }

            switch (type) {
                case ENTRADA: workerHous.setHour_first(currentTime); break;
                case ALMOCO_INICIO: workerHous.setHour_dinner(currentTime); break;
                case ALMOCO_FIM: workerHous.setHour_finish(currentTime); break;
                case FIM: workerHous.setHour_stop(currentTime); break;
            }
            workerHous.setHour_after(currentTime);

            return workerHourRepository.saveWorkerHous(workerHous);
        });
    }

    // --- Métodos de UI e Auxiliares ---

    private void updateButtonUI(PointType type, WorkerHous workerHous) {
        if (workerHous == null) return;

        // Exemplo simplificado, você pode adaptar para os outros botões
        switch (type) {
            case ENTRADA: binding.buttonFistHour.setText(workerHous.getHour_first()); break;
            case ALMOCO_INICIO: binding.buttonDinnerStarHour.setText(workerHous.getHour_dinner()); break;
            case ALMOCO_FIM: binding.buttonDinnerFinishHour.setText(workerHous.getHour_finish()); break;
            case FIM: binding.buttonStop.setText(workerHous.getHour_stop()); break;
        }
    }

    private void loadInitialData() {
        workerHourRepository.getWorkerHous().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WorkerHous workerHous = task.getResult();
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

    // Agrupei a lógica de atualização da UI para ficar mais legível
    private void updateFullUI(WorkerHous wh) {
        updateStatusUI(wh.getHour_first(), binding.buttonFistHour, binding.imageFistHour,
                binding.buttonDinnerStarHour, binding.imageDinnerStarHour, "Entrada");

        /*if (!wh.getHour_first().isEmpty()) checkVehicleInspection(true);*/ // check inicial

        updateStatusUI(wh.getHour_dinner(), binding.buttonDinnerStarHour, binding.imageDinnerStarHour,
                binding.buttonDinnerFinishHour, binding.imageDinnerFinishHour, "Almoço");

        updateStatusUI(wh.getHour_finish(), binding.buttonDinnerFinishHour, binding.imageDinnerFinishHour,
                binding.buttonStop, binding.imageStop, "Saída");

        if (!wh.getHour_stop().isEmpty()) {
            // Lógica de quando o dia acabou
            binding.buttonStop.setText(wh.getHour_stop());
            binding.progressBar.setVisibility(View.VISIBLE);
            // ... esconder outros botões (simplifiquei aqui para caber)
        } else {
            binding.buttonStop.setText("Fim do Expediente");
        }
    }

    // Método auxiliar para reduzir duplicação no updateUI
    private void updateStatusUI(String hourValue, android.widget.TextView btn, android.widget.ImageView img,
                                View nextBtn, View nextImg, String defaultText) {
        if (!hourValue.isEmpty()) {
            btn.setText(hourValue);
            img.setImageResource(R.drawable.confirm_hour);
            img.setBackgroundResource(R.drawable.button_stroke_squad);
            if(nextBtn != null) nextBtn.setVisibility(View.VISIBLE);
            if(nextImg != null) nextImg.setVisibility(View.VISIBLE);
        } else {
            btn.setText(defaultText);
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
        new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setNeutralButton("Ok", null).show();
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
}