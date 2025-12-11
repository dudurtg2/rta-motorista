package com.example.rta_app.SOLID.activitys;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.AdapterView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.rta_app.SOLID.api.CarroRotaRepository;
import com.example.rta_app.SOLID.api.VerificardorDeCarroRepository;
import com.example.rta_app.SOLID.api.WorkerHourRepository;
import com.example.rta_app.SOLID.entities.Carro;
import com.example.rta_app.SOLID.entities.VerificadoresDoCarro;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.databinding.ActivityCarroRotasBinding;

public class CarroRotasActivity extends AppCompatActivity {

    private static final String TAG = "CarroRotasActivityww";

    private ActivityCarroRotasBinding binding;

    private CarroRotaRepository carroRotaRepository;
    private VerificardorDeCarroRepository verificardorDeCarroRepository;
    private WorkerHourRepository workerHourRepository;

    private static final int REQ_FOTO_COMBUSTIVEL = 1;
    private static final int REQ_FOTO_PARABRISA = 2;
    private static final int REQ_FOTO_LATARIA = 3;
    private static final int REQ_FOTO_KILOMETRAGEM = 4;
    private static final int REQ_CAMERA_PERMISSION = 100;


    private int pendingRequestCodeForCamera = -1;

    private List<Carro> carro = new ArrayList<>();

    // Base64 das fotos (sem recompressão – bytes originais do arquivo)
    private String fotoCombustivelBase64;
    private String fotoParabrisaBase64;
    private String fotoLatariaBase64;
    private String fotokilometragemBase64;

    // Para capturar foto em alta resolução
    private Uri currentPhotoUri;
    private String currentPhotoPath;

    // Checklist mecânico (em grupos Ruim/Bom)
    private View[] mecanicaRows;
    private CheckBox[][] mecanicaChecks;
    private boolean isUpdatingGroup = false;
    private boolean checklistEnabled = false;

    // Labels dos itens (mesma ordem de mecanicaRows/mecanicaChecks)
    private static final String[] MECANICA_LABELS = {
            "Água do Radiador",
            "Óleo do Motor",
            "Óleo de Freio",
            "Óleo de Direção",
            "Faróis",
            "Piscas (Setas)",
            "Luz de Freio",
            "Luz de Ré",
            "Calibragem Pneus",
            "Step (Reserva)",
            "Chave de Rodas",
            "Macaco"
    };

    // Descrição dos defeitos (quando marcado Ruim)
    private String[] mecanicaDefeitos;

    private WorkerHous workerHous;

    // Lista de placas para o Spinner
    private final List<String> packingLocal = new ArrayList<>();
    private ArrayAdapter<String> placasAdapter;
    private String placaSelecionada; // placa atualmente selecionada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() iniciado");
        EdgeToEdge.enable(this);
        binding = ActivityCarroRotasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "Layout inflado e setContentView concluído");

        carroRotaRepository = new CarroRotaRepository(this);
        verificardorDeCarroRepository = new VerificardorDeCarroRepository(this);
        workerHourRepository = new WorkerHourRepository(this);
        Log.d(TAG, "Repositórios inicializados");

        setupChecklistMecanico();
        setupFotoButtons();
        setupSpinnerPlacas();

        Log.d(TAG, "Buscando WorkerHous atual");
        workerHourRepository.getWorkerHous()
                .addOnSuccessListener(t -> {
                    workerHous = t;
                    Log.d(TAG, "getWorkerHous() sucesso: workerHous=" + workerHous);
                    if (t.getCarroInicial()) {
                        Log.d(TAG, "Carro já inicializado para este usuário. Ocultando spinner e habilitando checklist");
                        binding.dishesCategorySpinner.setVisibility(View.GONE);
                        resetAndEnableChecklist();
                    } else {
                        Log.d(TAG, "Carro ainda não inicializado para este usuário. Checklist aguardando seleção de placa");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao carregar WorkerHous", e);
                    Toast.makeText(this, "Erro ao carregar dados do colaborador", Toast.LENGTH_SHORT).show();
                });

        Log.d(TAG, "Buscando lista de carros");
        carroRotaRepository.findAll()
                .addOnSuccessListener(carros -> {
                    Log.d(TAG, "findAll() de CarroRotaRepository retornou " + carros.size() + " carros");
                    carro = carros;
                    packingLocal.clear();
                    packingLocal.add("Selecione o veículo");
                    for (Carro c : carros) {
                        Log.d(TAG, "Carro encontrado: id=" + c.getId() + ", placa=" + c.getPlaca());
                        packingLocal.add(c.getPlaca());
                    }
                    placasAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Adapter de placas atualizado");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar carros", e);
                    Toast.makeText(this, "Erro ao buscar carros", Toast.LENGTH_SHORT).show();
                });
    }

    // -----------------------------
    // SPINNER DE PLACAS
    // -----------------------------
    private void setupSpinnerPlacas() {
        Log.d(TAG, "setupSpinnerPlacas()");
        placasAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                packingLocal
        );

        binding.dishesCategorySpinner.setAdapter(placasAdapter);
        binding.dishesCategorySpinner.setPrompt("Selecione o veículo");

        binding.dishesCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Spinner onItemSelected: position=" + position);
                if (packingLocal.isEmpty()) {
                    Log.w(TAG, "packingLocal vazio ao selecionar spinner");
                    return;
                }

                String item = packingLocal.get(position);
                Log.d(TAG, "Item selecionado no spinner: " + item);

                if (position == 0) {
                    Log.d(TAG, "Placeholder selecionado. Desativando checklist");
                    placaSelecionada = null;
                    disableChecklist();
                    return;
                }

                if (!item.equals(placaSelecionada)) {
                    Log.d(TAG, "Nova placa selecionada: " + item + " (anterior=" + placaSelecionada + ")");
                    placaSelecionada = item;
                    resetAndEnableChecklist();
                } else {
                    Log.d(TAG, "Mesma placa selecionada novamente: " + item);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "Spinner onNothingSelected");
                placaSelecionada = null;
                disableChecklist();
            }
        });
    }

    // -----------------------------
    // CHECKLIST MECÂNICO (Ruim/Bom em cascata)
    // -----------------------------
    private void setupChecklistMecanico() {
        Log.d(TAG, "setupChecklistMecanico()");
        mecanicaRows = new View[]{
                binding.rowAgua,
                binding.rowOleoMotor,
                binding.rowOleoFreio,
                binding.rowOleoDirecao,
                binding.rowFarois,
                binding.rowPiscas,
                binding.rowLuzFreio,
                binding.rowLuzRe,
                binding.rowPneus,
                binding.rowStep,
                binding.rowChaveRodas,
                binding.rowMacaco
        };

        mecanicaChecks = new CheckBox[][]{
                {binding.chkAguaRuim, binding.chkAguaBom},
                {binding.chkOleoMotorRuim, binding.chkOleoMotorBom},
                {binding.chkOleoFreioRuim, binding.chkOleoFreioBom},
                {binding.chkOleoDirecaoRuim, binding.chkOleoDirecaoBom},
                {binding.chkFaroisRuim, binding.chkFaroisBom},
                {binding.chkPiscasRuim, binding.chkPiscasBom},
                {binding.chkLuzFreioRuim, binding.chkLuzFreioBom},
                {binding.chkLuzReRuim, binding.chkLuzReBom},
                {binding.chkPneusRuim, binding.chkPneusBom},
                {binding.chkStepRuim, binding.chkStepBom},
                {binding.chkChaveRodasRuim, binding.chkChaveRodasBom},
                {binding.chkMacacoRuim, binding.chkMacacoBom}
        };

        mecanicaDefeitos = new String[mecanicaChecks.length];
        Log.d(TAG, "Checklist mecânico possui " + mecanicaChecks.length + " itens");

        // No início: checklist desabilitado e tudo oculto
        for (View row : mecanicaRows) {
            row.setVisibility(View.GONE);
        }
        binding.lblFotos.setVisibility(View.GONE);
        binding.layoutFotos.setVisibility(View.GONE);
        binding.btnFinalizar.setVisibility(View.GONE);

        // Listeners Ruim/Bom por grupo
        for (int groupIndex = 0; groupIndex < mecanicaChecks.length; groupIndex++) {
            final int idx = groupIndex;
            CheckBox ruim = mecanicaChecks[groupIndex][0];
            CheckBox bom = mecanicaChecks[groupIndex][1];

            int finalIdx = idx;
            ruim.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "onRuimChanged(idx=" + finalIdx + ", isChecked=" + isChecked + ")");
                onRuimChanged(finalIdx, isChecked);
            });

            bom.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "onBomChanged(idx=" + finalIdx + ", isChecked=" + isChecked + ")");
                onBomChanged(finalIdx, isChecked);
            });
        }

        checklistEnabled = false; // só fica true após escolher veículo
        Log.d(TAG, "Checklist inicializado desabilitado");
    }

    // Clique em "Ruim"
    private void onRuimChanged(int index, boolean isChecked) {
        Log.d(TAG, "onRuimChanged() chamado, index=" + index + ", isChecked=" + isChecked +
                ", checklistEnabled=" + checklistEnabled + ", isUpdatingGroup=" + isUpdatingGroup);
        if (!checklistEnabled || isUpdatingGroup) return;

        if (isChecked) {
            isUpdatingGroup = true;
            mecanicaChecks[index][0].setChecked(false);
            isUpdatingGroup = false;
            Log.d(TAG, "Abrindo diálogo de confirmação de defeito para index=" + index);
            showConfirmDefeitoDialog(index);
        } else {
            Log.d(TAG, "Ruim desmarcado manualmente. Reprocessando grupo index=" + index);
            processGroupChange(index);
        }
    }

    // Clique em "Bom"
    private void onBomChanged(int index, boolean isChecked) {
        Log.d(TAG, "onBomChanged() chamado, index=" + index + ", isChecked=" + isChecked +
                ", checklistEnabled=" + checklistEnabled + ", isUpdatingGroup=" + isUpdatingGroup);
        if (!checklistEnabled || isUpdatingGroup) return;

        if (isChecked) {
            Log.d(TAG, "Marcado Bom para index=" + index + ". Desmarcando Ruim e limpando defeito");
            isUpdatingGroup = true;
            mecanicaChecks[index][0].setChecked(false);
            isUpdatingGroup = false;

            mecanicaDefeitos[index] = null;
            processGroupChange(index);
        } else {
            Log.d(TAG, "Bom desmarcado manualmente. Reprocessando grupo index=" + index);
            processGroupChange(index);
        }
    }

    // Processa o estado do grupo após qualquer alteração
    private void processGroupChange(int index) {
        Log.d(TAG, "processGroupChange() index=" + index);
        CheckBox[] grupo = mecanicaChecks[index];
        boolean anyChecked = grupo[0].isChecked() || grupo[1].isChecked();
        Log.d(TAG, "Grupo index=" + index + " anyChecked=" + anyChecked);

        if (anyChecked) {
            if (index + 1 < mecanicaRows.length) {
                Log.d(TAG, "Mostrando próxima row index=" + (index + 1));
                mecanicaRows[index + 1].setVisibility(View.VISIBLE);
            } else {
                Log.d(TAG, "Último item respondido. Mostrando sessão de fotos e botão finalizar");
                binding.lblFotos.setVisibility(View.VISIBLE);
                binding.layoutFotos.setVisibility(View.VISIBLE);
                binding.btnFinalizar.setVisibility(View.VISIBLE);
            }
        } else {
            Log.d(TAG, "Nenhum checkbox marcado no grupo index=" + index + ". Escondendo grupos abaixo");
            esconderDosGrupos(index + 1);
        }
    }

    // Esconde todas as linhas a partir de fromIndex, limpa checks e fotos/botão
    private void esconderDosGrupos(int fromIndex) {
        Log.d(TAG, "esconderDosGrupos() fromIndex=" + fromIndex);
        for (int i = fromIndex; i < mecanicaRows.length; i++) {
            Log.d(TAG, "Escondendo row index=" + i);
            mecanicaRows[i].setVisibility(View.GONE);
            for (CheckBox cb : mecanicaChecks[i]) {
                cb.setChecked(false);
            }
            mecanicaDefeitos[i] = null;
        }

        Log.d(TAG, "Escondendo fotos e botão finalizar");
        binding.lblFotos.setVisibility(View.GONE);
        binding.layoutFotos.setVisibility(View.GONE);
        binding.btnFinalizar.setVisibility(View.GONE);
    }

    // Reset total do checklist quando muda a placa e habilita o fluxo
    private void resetAndEnableChecklist() {
        Log.d(TAG, "resetAndEnableChecklist() chamado. Placa selecionada=" + placaSelecionada);
        isUpdatingGroup = true;

        for (int i = 0; i < mecanicaChecks.length; i++) {
            for (CheckBox cb : mecanicaChecks[i]) {
                cb.setChecked(false);
            }
            mecanicaDefeitos[i] = null;
        }

        for (View row : mecanicaRows) {
            row.setVisibility(View.GONE);
        }

        binding.lblFotos.setVisibility(View.GONE);
        binding.layoutFotos.setVisibility(View.GONE);
        binding.btnFinalizar.setVisibility(View.GONE);

        isUpdatingGroup = false;
        checklistEnabled = true;
        Log.d(TAG, "Checklist habilitado");

        if (mecanicaRows.length > 0) {
            Log.d(TAG, "Mostrando primeira row do checklist");
            mecanicaRows[0].setVisibility(View.VISIBLE);
        }
    }

    // Desativa totalmente o checklist (quando não há carro selecionado)
    private void disableChecklist() {
        Log.d(TAG, "disableChecklist() chamado");
        isUpdatingGroup = true;

        for (int i = 0; i < mecanicaChecks.length; i++) {
            for (CheckBox cb : mecanicaChecks[i]) {
                cb.setChecked(false);
            }
            mecanicaDefeitos[i] = null;
        }

        for (View row : mecanicaRows) {
            row.setVisibility(View.GONE);
        }

        binding.lblFotos.setVisibility(View.GONE);
        binding.layoutFotos.setVisibility(View.GONE);
        binding.btnFinalizar.setVisibility(View.GONE);

        isUpdatingGroup = false;
        checklistEnabled = false;
        Log.d(TAG, "Checklist desabilitado e tudo oculto");
    }

    // -----------------------------
    // DIÁLOGOS DE "RUIM"
    // -----------------------------
    private void showConfirmDefeitoDialog(int index) {
        String label = MECANICA_LABELS[index];
        Log.d(TAG, "showConfirmDefeitoDialog() para index=" + index + ", label=" + label);

        new AlertDialog.Builder(this)
                .setTitle("Confirmar defeito")
                .setMessage("Confirmar que \"" + label + "\" está com problema?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    Log.d(TAG, "Usuário confirmou defeito para index=" + index + ", label=" + label);
                    showDescricaoDefeitoDialog(index);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    Log.d(TAG, "Usuário cancelou confirmação de defeito para index=" + index + ", label=" + label);
                })
                .show();
    }

    private void showDescricaoDefeitoDialog(int index) {
        String label = MECANICA_LABELS[index];
        Log.d(TAG, "showDescricaoDefeitoDialog() para index=" + index + ", label=" + label);

        final EditText input = new EditText(this);
        input.setHint("Descreva o defeito...");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle(label)
                .setMessage("Descreva o problema encontrado:")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String desc = input.getText().toString().trim();
                    mecanicaDefeitos[index] = desc;
                    Log.d(TAG, "Descrição de defeito salva para index=" + index + ": " + desc);

                    isUpdatingGroup = true;
                    mecanicaChecks[index][0].setChecked(true);  // Ruim
                    mecanicaChecks[index][1].setChecked(false); // Bom
                    isUpdatingGroup = false;

                    saveDefeitos(desc);
                    processGroupChange(index);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    Log.d(TAG, "Usuário cancelou descrição de defeito para index=" + index);
                })
                .show();
    }

    private void finalizarPontoEVoltar(String mensagemSucesso) {
        Log.d(TAG, "finalizarPontoEVoltar() chamado. mensagemSucesso=" + mensagemSucesso +
                ", workerHous=" + workerHous);
        workerHourRepository.saveWorkerHous(workerHous)
                .addOnSuccessListener(t -> {
                    Log.d(TAG, "saveWorkerHous() sucesso");
                    Toast.makeText(this, mensagemSucesso, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    Log.d(TAG, "Chamando finishAffinity()");
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao registrar hora no saveWorkerHous()", e);
                    Toast.makeText(this, "Erro ao registrar hora: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private Long getCarroSelecionadoId() {
        Log.d(TAG, "getCarroSelecionadoId() chamado. workerHous=" + workerHous +
                ", workerHous.getIdCarro()=" + (workerHous != null ? workerHous.getIdCarro() : null) +
                ", placaSelecionada=" + placaSelecionada);

        // 1) Se já tem o id salvo no WorkerHous, usa ele
        if (workerHous.getIdCarro() > 0) {
            Log.d(TAG, "Retornando idCarro já salvo em workerHous: " + workerHous.getIdCarro());
            return workerHous.getIdCarro();
        }

        // 2) Garante que o usuário realmente escolheu um veículo
        if (placaSelecionada == null) {
            Log.e(TAG, "getCarroSelecionadoId(): placaSelecionada é null. Nenhum veículo selecionado.");
            throw new IllegalStateException("Nenhum veículo selecionado.");
        }

        // 3) Procura o carro pela placa, evitando NPE
        Long carroId = carro.stream()
                .filter(c -> placaSelecionada.equals(c.getPlaca())) // evita NPE em getPlaca()
                .map(Carro::getId)
                .findFirst()
                .orElseThrow(() -> {
                    Log.e(TAG, "Placa não encontrada na lista de carros: " + placaSelecionada);
                    return new IllegalStateException("Veículo não encontrado para placa: " + placaSelecionada);
                });

        Log.d(TAG, "Carro encontrado pelo spinner: placa=" + placaSelecionada + ", id=" + carroId);
        return carroId;
    }


    private void saveDefeitos(String descricao) {
        Log.d(TAG, "saveDefeitos() chamado. descricao=" + descricao +
                ", workerHous=" + workerHous);

        boolean deveAtualizar = workerHous != null && workerHous.getCarroInicial();
        Log.d(TAG, "deveAtualizar=" + deveAtualizar);

        VerificadoresDoCarro verificadoresDoCarro =
                new VerificadoresDoCarro(
                        "QUEBRADO",
                        false,   // verificadorInicial?
                        false,   // verificadorFinal?
                        null,
                        null,
                        true,    // finalizado?
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        descricao,
                        null,
                        getCarroSelecionadoId()
                );

        Log.d(TAG, "Objeto VerificadoresDoCarro (QUEBRADO) montado: " + verificadoresDoCarro);



        if (deveAtualizar) {
            Log.d(TAG, "Atualizando vistoria existente com defeito (updateF)");
            workerHous.setCarroFinal(true);

            verificardorDeCarroRepository.updateF(verificadoresDoCarro, workerHous.getIdVerificardor())
                    .addOnSuccessListener(a -> {
                        Log.d(TAG, "updateF() sucesso ao salvar defeitos");
                        Toast.makeText(this, "Defeitos registrados e vistoria atualizada", Toast.LENGTH_SHORT).show();
                        finalizarPontoEVoltar("Hora final registrada");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erro ao salvar defeitos em updateF()", e);
                        Toast.makeText(this, "Erro ao salvar defeitos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } else {
            Log.d(TAG, "Criando nova vistoria com defeito (save)");

            verificardorDeCarroRepository.save(verificadoresDoCarro)
                    .addOnSuccessListener(id -> {
                        Log.d(TAG, "save() sucesso, idVerificador=" + id);

                        Toast.makeText(this, "Defeitos registrados e vistoria criada", Toast.LENGTH_SHORT).show();
                        finalizarPontoEVoltar("Hora final registrada");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erro ao salvar defeitos em save()", e);
                        Toast.makeText(this, "Erro ao salvar defeitos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupFotoButtons() {
        Log.d(TAG, "setupFotoButtons()");
        binding.btnFotoCombustivel.setOnClickListener(v -> {
            Log.d(TAG, "Clique em btnFotoCombustivel");
            abrirCamera(REQ_FOTO_COMBUSTIVEL);
        });

        binding.btnFotoParabrisa.setOnClickListener(v -> {
            Log.d(TAG, "Clique em btnFotoParabrisa");
            abrirCamera(REQ_FOTO_PARABRISA);
        });

        binding.btnFotoLataria.setOnClickListener(v -> {
            Log.d(TAG, "Clique em btnFotoLataria");
            abrirCamera(REQ_FOTO_LATARIA);
        });

        binding.btnFotoKilometragem.setOnClickListener(v -> {
            Log.d(TAG, "Clique em btnFotoKilometragem");
            abrirCamera(REQ_FOTO_KILOMETRAGEM);
        });

        binding.btnFinalizar.setOnClickListener(v -> {
            Log.d(TAG, "Clique em btnFinalizar. workerHous=" + workerHous);
            binding.progressBarCarros.setVisibility(View.VISIBLE);
            binding.btnFinalizar.setVisibility(View.GONE);

            if (workerHous != null && workerHous.getCarroInicial()) {
                Log.d(TAG, "Fluxo de vistoria final (carroInicial == true)");

                VerificadoresDoCarro verFinal = new VerificadoresDoCarro(
                        "LIVRE",
                        null,
                        true,
                        null,
                        null,
                        true,
                        null,
                        fotoCombustivelBase64,
                        null,
                        fotoParabrisaBase64,
                        null,
                        fotoLatariaBase64,
                        null,
                        fotokilometragemBase64,
                        null,
                        null,
                        workerHous.getIdCarro()
                );

                Log.d(TAG, "Objeto VerificadoresDoCarro (LIVRE) montado: " + verFinal);

                verificardorDeCarroRepository.update(verFinal, workerHous.getIdVerificardor())
                        .addOnSuccessListener(a -> {
                            Log.d(TAG, "update() sucesso na vistoria final");
                            Toast.makeText(this, "Vistoria finalizada", Toast.LENGTH_SHORT).show();
                            try {
                                workerHous.setCarroFinal(true);
                                Log.d(TAG, "workerHous.setCarroFinal(true)");
                                finalizarPontoEVoltar("Hora final registrada");
                            } catch (Exception e) {
                                Log.e(TAG, "Erro ao registrar hora final após update()", e);
                                binding.progressBarCarros.setVisibility(View.GONE);
                                binding.btnFinalizar.setVisibility(View.VISIBLE);
                                Toast.makeText(this, "Erro ao registrar hora final: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Erro ao salvar vistoria final em update()", e);
                            binding.progressBarCarros.setVisibility(View.GONE);
                            binding.btnFinalizar.setVisibility(View.VISIBLE);
                            Toast.makeText(this, "Erro ao salvar vistoria: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

            } else {
                Log.d(TAG, "Fluxo de vistoria inicial (carroInicial == false ou workerHous null)");

                VerificadoresDoCarro verInicial = new VerificadoresDoCarro(
                        "EM_USO",
                        true,
                        null,
                        null,
                        null,
                        false,
                        fotoCombustivelBase64,
                        null,
                        fotoParabrisaBase64,
                        null,
                        fotoLatariaBase64,
                        null,
                        fotokilometragemBase64,
                        null,
                        null,
                        null,
                        getCarroSelecionadoId()
                );

                Log.d(TAG, "Objeto VerificadoresDoCarro (EM_USO) montado: " + verInicial);

                verificardorDeCarroRepository.save(verInicial)
                        .addOnSuccessListener(id -> {
                            Log.d(TAG, "save() sucesso na vistoria inicial. id=" + id);
                            Toast.makeText(this, "Vistoria inicial registrada", Toast.LENGTH_SHORT).show();
                            try {
                                workerHous.setCarroInicial(true);
                                workerHous.setCarroFinal(false);
                                workerHous.setIdVerificardor(id);
                                workerHous.setIdCarro(getCarroSelecionadoId());
                                Log.d(TAG, "workerHous atualizado com dados de vistoria inicial: " + workerHous);
                                finalizarPontoEVoltar("Hora inicial registrada");
                            } catch (Exception e) {
                                Log.e(TAG, "Erro ao registrar hora inicial após save()", e);
                                binding.progressBarCarros.setVisibility(View.GONE);
                                binding.btnFinalizar.setVisibility(View.VISIBLE);
                                Toast.makeText(this, "Erro ao registrar hora inicial: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Erro ao salvar vistoria inicial em save()", e);
                            binding.btnFinalizar.setVisibility(View.VISIBLE);
                            binding.progressBarCarros.setVisibility(View.GONE);
                            Toast.makeText(this, "Erro ao salvar vistoria: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    // Abre câmera usando FileProvider e salva foto em ARQUIVO (sem thumbnail 192x192)
    private void abrirCamera(int requestCode) {
        Log.d(TAG, "abrirCamera() requestCode=" + requestCode);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Permissão de câmera ainda não concedida. Solicitando...");
            pendingRequestCodeForCamera = requestCode;

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQ_CAMERA_PERMISSION
            );
            return;
        }

        Log.d(TAG, "Permissão de câmera já concedida. Abrindo Intent ACTION_IMAGE_CAPTURE");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                Log.d(TAG, "Arquivo de foto criado em: " + photoFile.getAbsolutePath());
                if (photoFile != null) {
                    currentPhotoPath = photoFile.getAbsolutePath();

                    currentPhotoUri = FileProvider.getUriForFile(
                            this,
                            "com.example.rta_app.provider",
                            photoFile
                    );

                    Log.d(TAG, "Uri para FileProvider: " + currentPhotoUri);

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivityForResult(intent, requestCode);
                    Log.d(TAG, "startActivityForResult() disparado para requestCode=" + requestCode);
                }
            } catch (IOException e) {
                Log.e(TAG, "Erro ao criar arquivo de imagem em abrirCamera()", e);
                Toast.makeText(this, "Erro ao criar arquivo de imagem", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Nenhum app de câmera disponível para ACTION_IMAGE_CAPTURE");
            Toast.makeText(this, "Não foi possível abrir a câmera", Toast.LENGTH_SHORT).show();
        }
    }

    // Cria arquivo temporário para a foto
    private File createImageFile() throws IOException {
        Log.d(TAG, "createImageFile() chamado");
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new java.util.Date());
        String imageFileName = "FOTO_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.d(TAG, "Diretório de armazenamento: " + storageDir);
        File file = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        Log.d(TAG, "Arquivo temporário de imagem criado: " + file.getAbsolutePath());
        return file;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult() requestCode=" + requestCode);

        if (requestCode == REQ_CAMERA_PERMISSION) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Permissão de câmera retornada: granted=" + granted +
                    ", pendingRequestCodeForCamera=" + pendingRequestCodeForCamera);

            if (granted) {
                if (pendingRequestCodeForCamera != -1) {
                    Log.d(TAG, "Chamando abrirCamera() novamente para requestCode pendente=" + pendingRequestCodeForCamera);
                    abrirCamera(pendingRequestCodeForCamera);
                    pendingRequestCodeForCamera = -1;
                }
            } else {
                Log.w(TAG, "Permissão de câmera negada pelo usuário");
                Toast.makeText(this, "Permissão de câmera negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult() requestCode=" + requestCode + ", resultCode=" + resultCode +
                ", currentPhotoPath=" + currentPhotoPath);

        if (resultCode != Activity.RESULT_OK || currentPhotoPath == null) {
            Log.w(TAG, "onActivityResult ignorado: resultCode != RESULT_OK ou currentPhotoPath null");
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        if (bitmap == null) {
            Log.e(TAG, "Bitmap retornou null ao tentar decodificar arquivo: " + currentPhotoPath);
            Toast.makeText(this, "Erro ao carregar foto", Toast.LENGTH_SHORT).show();
            return;
        }

        String base64 = fileToBase64(currentPhotoPath);
        Log.d(TAG, "Foto convertida para Base64 (length=" + (base64 != null ? base64.length() : -1) + ") para requestCode=" + requestCode);

        switch (requestCode) {
            case REQ_FOTO_COMBUSTIVEL:
                Log.d(TAG, "Assinando fotoCombustivelBase64 & preview");
                fotoCombustivelBase64 = base64;
                binding.imgPreviewCombustivel.setVisibility(View.VISIBLE);
                binding.imgPreviewCombustivel.setImageBitmap(bitmap);
                break;

            case REQ_FOTO_PARABRISA:
                Log.d(TAG, "Assinando fotoParabrisaBase64 & preview");
                fotoParabrisaBase64 = base64;
                binding.imgPreviewParabrisa.setVisibility(View.VISIBLE);
                binding.imgPreviewParabrisa.setImageBitmap(bitmap);
                break;

            case REQ_FOTO_LATARIA:
                Log.d(TAG, "Assinando fotoLatariaBase64 & preview");
                fotoLatariaBase64 = base64;
                binding.imgPreviewLataria.setVisibility(View.VISIBLE);
                binding.imgPreviewLataria.setImageBitmap(bitmap);
                break;

            case REQ_FOTO_KILOMETRAGEM:
                Log.d(TAG, "Assinando fotokilometragemBase64 & preview");
                fotokilometragemBase64 = base64;
                binding.imgPreviewKilometragem.setVisibility(View.VISIBLE);
                binding.imgPreviewKilometragem.setImageBitmap(bitmap);
                break;

            default:
                Log.w(TAG, "requestCode desconhecido em onActivityResult(): " + requestCode);
                break;
        }
    }


    private String fileToBase64(String path) {
        Log.d(TAG, "fileToBase64() chamado para path=" + path);
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap == null) {
                Log.e(TAG, "Bitmap é null em fileToBase64() para path=" + path);
                return null;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            Log.d(TAG, "Bitmap comprimido em JPEG qualidade 50");

            bitmap.recycle();
            Log.d(TAG, "Bitmap reciclado para liberar memória");

            byte[] bytes = baos.toByteArray();
            String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            Log.d(TAG, "Base64 gerado com length=" + base64.length());
            return base64;

        } catch (Exception e) {
            Log.e(TAG, "Erro em fileToBase64() para path=" + path, e);
            return null;
        }
    }

}
