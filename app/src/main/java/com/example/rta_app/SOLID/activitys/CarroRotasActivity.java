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
        EdgeToEdge.enable(this);
        binding = ActivityCarroRotasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        carroRotaRepository = new CarroRotaRepository(this);
        verificardorDeCarroRepository = new VerificardorDeCarroRepository(this);
        workerHourRepository = new WorkerHourRepository(this);


        setupChecklistMecanico();
        setupFotoButtons();
        setupSpinnerPlacas();
        workerHourRepository.getWorkerHous().addOnSuccessListener(t -> {
            workerHous = t;
            if(t.getCarroInicial()){

                binding.dishesCategorySpinner.setVisibility(View.GONE);
                resetAndEnableChecklist();
            }
        });




        carroRotaRepository.findAll()
                .addOnSuccessListener(carros -> {
                    carro = carros;
                    packingLocal.clear();
                    // Placeholder na posição 0
                    packingLocal.add("Selecione o veículo");
                    for (Carro c : carros) {
                        packingLocal.add(c.getPlaca());
                    }
                    placasAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("CarroRota", "Erro ao buscar carros", e);
                    Toast.makeText(this, "Erro ao buscar carros", Toast.LENGTH_SHORT).show();
                });


    }

    // -----------------------------
    // SPINNER DE PLACAS
    // -----------------------------
    private void setupSpinnerPlacas() {
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
                if (packingLocal.isEmpty()) return;

                String item = packingLocal.get(position);

                if (position == 0) {
                    // Placeholder → não libera checklist
                    placaSelecionada = null;
                    disableChecklist();
                    return;
                }

                if (!item.equals(placaSelecionada)) {
                    placaSelecionada = item;
                    Log.d("CarroRota", "Placa selecionada: " + placaSelecionada);
                    resetAndEnableChecklist();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                placaSelecionada = null;
                disableChecklist();
            }
        });
    }

    // -----------------------------
    // CHECKLIST MECÂNICO (Ruim/Bom em cascata)
    // -----------------------------
    private void setupChecklistMecanico() {
        // Linhas (rows) na mesma ordem que o XML
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

        // Para cada linha, o par [Ruim, Bom] (na mesma ordem das labels)
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

            ruim.setOnCheckedChangeListener((buttonView, isChecked) ->
                    onRuimChanged(idx, isChecked));

            bom.setOnCheckedChangeListener((buttonView, isChecked) ->
                    onBomChanged(idx, isChecked));
        }

        checklistEnabled = false; // só fica true após escolher veículo
    }

    // Clique em "Ruim"
    private void onRuimChanged(int index, boolean isChecked) {
        if (!checklistEnabled || isUpdatingGroup) return;

        if (isChecked) {
            // Reverte imediatamente o check e abre diálogos
            isUpdatingGroup = true;
            mecanicaChecks[index][0].setChecked(false);
            isUpdatingGroup = false;

            showConfirmDefeitoDialog(index);
        } else {
            // Desmarcou Ruim manualmente → recalcula grupo
            processGroupChange(index);
        }
    }

    // Clique em "Bom"
    private void onBomChanged(int index, boolean isChecked) {
        if (!checklistEnabled || isUpdatingGroup) return;

        if (isChecked) {
            isUpdatingGroup = true;
            // Exclusividade: se marcou Bom, desmarca Ruim
            mecanicaChecks[index][0].setChecked(false);
            isUpdatingGroup = false;

            // Limpa eventual descrição de defeito
            mecanicaDefeitos[index] = null;

            processGroupChange(index);
        } else {
            // Desmarcou Bom → recalcula
            processGroupChange(index);
        }
    }

    // Processa o estado do grupo após qualquer alteração
    private void processGroupChange(int index) {
        CheckBox[] grupo = mecanicaChecks[index];
        boolean anyChecked = grupo[0].isChecked() || grupo[1].isChecked();

        if (anyChecked) {
            // Grupo respondido → mostra próximo ou libera fotos/botão
            if (index + 1 < mecanicaRows.length) {
                mecanicaRows[index + 1].setVisibility(View.VISIBLE);
            } else {
                binding.lblFotos.setVisibility(View.VISIBLE);
                binding.layoutFotos.setVisibility(View.VISIBLE);
                binding.btnFinalizar.setVisibility(View.VISIBLE);
            }
        } else {
            // Grupo sem nada marcado → esconde abaixo
            esconderDosGrupos(index + 1);
        }
    }

    // Esconde todas as linhas a partir de fromIndex, limpa checks e fotos/botão
    private void esconderDosGrupos(int fromIndex) {
        for (int i = fromIndex; i < mecanicaRows.length; i++) {
            mecanicaRows[i].setVisibility(View.GONE);
            for (CheckBox cb : mecanicaChecks[i]) {
                cb.setChecked(false);
            }
            mecanicaDefeitos[i] = null;
        }

        binding.lblFotos.setVisibility(View.GONE);
        binding.layoutFotos.setVisibility(View.GONE);
        binding.btnFinalizar.setVisibility(View.GONE);
    }

    // Reset total do checklist quando muda a placa e habilita o fluxo
    private void resetAndEnableChecklist() {
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

        // Mostra apenas a primeira linha
        if (mecanicaRows.length > 0) {
            mecanicaRows[0].setVisibility(View.VISIBLE);
        }
    }

    // Desativa totalmente o checklist (quando não há carro selecionado)
    private void disableChecklist() {
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
    }

    // -----------------------------
    // DIÁLOGOS DE "RUIM"
    // -----------------------------
    private void showConfirmDefeitoDialog(int index) {
        String label = MECANICA_LABELS[index];

        new AlertDialog.Builder(this)
                .setTitle("Confirmar defeito")
                .setMessage("Confirmar que \"" + label + "\" está com problema?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    showDescricaoDefeitoDialog(index);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    // Não marca nada
                })
                .show();
    }

    private void showDescricaoDefeitoDialog(int index) {
        String label = MECANICA_LABELS[index];

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

                    // Marca Ruim de fato e processa o grupo
                    isUpdatingGroup = true;
                    mecanicaChecks[index][0].setChecked(true);  // Ruim
                    mecanicaChecks[index][1].setChecked(false); // Bom
                    isUpdatingGroup = false;

                    saveDefeitos(desc);

                    processGroupChange(index);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    // Não marca nada
                })
                .show();
    }

    private void saveDefeitos(String descricao) {
        VerificadoresDoCarro verificadoresDoCarro =
                new VerificadoresDoCarro(
                        "QUEBRADO",
                        false,
                        false,
                        null,
                        null,
                        true,
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
                        carro.stream()
                                .filter(v -> v.getPlaca().equals(placaSelecionada))
                                .findFirst()
                                .get()
                                .getId()
                );
        workerHous.setCarroFinal(true);

        workerHourRepository.saveWorkerHous(workerHous).addOnSuccessListener(t -> {
            Toast.makeText(this, "Hora inicial registrada", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finishAffinity(); // API 16+

        });

        verificardorDeCarroRepository.save(verificadoresDoCarro);
        finish();
    }


    private void setupFotoButtons() {
        binding.btnFotoCombustivel.setOnClickListener(v -> abrirCamera(REQ_FOTO_COMBUSTIVEL));
        binding.btnFotoParabrisa.setOnClickListener(v -> abrirCamera(REQ_FOTO_PARABRISA));
        binding.btnFotoLataria.setOnClickListener(v -> abrirCamera(REQ_FOTO_LATARIA));
        binding.btnFotoKilometragem.setOnClickListener(v -> abrirCamera(REQ_FOTO_KILOMETRAGEM));

        binding.btnFinalizar.setOnClickListener(v -> {
            binding.progressBarCarros.setVisibility(View.VISIBLE);

            binding.btnFinalizar.setVisibility(View.GONE);
            if (workerHous.getCarroInicial()) {
                verificardorDeCarroRepository.update(new VerificadoresDoCarro(
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
                        null
                ),workerHous.getIdVerificardor()).addOnSuccessListener(a -> {
                    Toast.makeText(this, "Vistoria finalizada", Toast.LENGTH_SHORT).show();
                    try {
                        workerHous.setCarroFinal(true);

                        workerHourRepository.saveWorkerHous(workerHous).addOnSuccessListener(t -> {
                            Toast.makeText(this, "Hora inicial registrada", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finishAffinity(); // API 16+

                        });
                    } catch (Exception e) {
                        Toast.makeText(this, "Erro ao registrar hora inicial: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }).addOnFailureListener(e -> {
                    binding.progressBarCarros.setVisibility(View.GONE);

                    binding.btnFinalizar.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Erro ao salvar vistoria: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } else {verificardorDeCarroRepository.save(new VerificadoresDoCarro(
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
                    carro.stream()
                            .filter(c -> c.getPlaca().equals(placaSelecionada))
                            .findFirst()
                            .get()
                            .getId()
            )).addOnSuccessListener(a -> {

                Toast.makeText(this, "Vistoria finalizada", Toast.LENGTH_SHORT).show();
                try {
                    workerHous.setCarroInicial(true);
                    workerHous.setCarroFinal(false);
                    workerHous.setIdVerificardor(a);
                    workerHourRepository.saveWorkerHous(workerHous).addOnSuccessListener(t -> {
                        Toast.makeText(this, "Hora inicial registrada", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(this, MainActivity.class));
                        finishAffinity(); // API 16+

                    });
                } catch (Exception e) {
                    Toast.makeText(this, "Erro ao registrar hora inicial: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }).addOnFailureListener(e -> {
                binding.btnFinalizar.setVisibility(View.VISIBLE);

                binding.progressBarCarros.setVisibility(View.GONE);
                Toast.makeText(this, "Erro ao salvar vistoria: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });}

        });
    }

    // Abre câmera usando FileProvider e salva foto em ARQUIVO (sem thumbnail 192x192)
    private void abrirCamera(int requestCode) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            pendingRequestCodeForCamera = requestCode;

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQ_CAMERA_PERMISSION
            );
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                if (photoFile != null) {
                    currentPhotoPath = photoFile.getAbsolutePath();

                    // AQUI usamos exatamente a authority do Manifest:
                    currentPhotoUri = FileProvider.getUriForFile(
                            this,
                            "com.example.rta_app.provider", // <- IGUAL ao Manifest
                            photoFile
                    );

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivityForResult(intent, requestCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Erro ao criar arquivo de imagem", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Não foi possível abrir a câmera", Toast.LENGTH_SHORT).show();
        }
    }

    // Cria arquivo temporário para a foto
    private File createImageFile() throws IOException {
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new java.util.Date());
        String imageFileName = "FOTO_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (pendingRequestCodeForCamera != -1) {
                    abrirCamera(pendingRequestCodeForCamera);
                    pendingRequestCodeForCamera = -1;
                }
            } else {
                Toast.makeText(this, "Permissão de câmera negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK || currentPhotoPath == null) return;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        if (bitmap == null) {
            Toast.makeText(this, "Erro ao carregar foto", Toast.LENGTH_SHORT).show();
            return;
        }

        // Base64 sem recompressão (opcional, se quiser)
        String base64 = fileToBase64(currentPhotoPath);

        switch (requestCode) {
            case REQ_FOTO_COMBUSTIVEL:
                fotoCombustivelBase64 = base64;
                binding.imgPreviewCombustivel.setVisibility(View.VISIBLE);
                binding.imgPreviewCombustivel.setImageBitmap(bitmap);
                break;

            case REQ_FOTO_PARABRISA:
                fotoParabrisaBase64 = base64;
                binding.imgPreviewParabrisa.setVisibility(View.VISIBLE);
                binding.imgPreviewParabrisa.setImageBitmap(bitmap);
                break;

            case REQ_FOTO_LATARIA:
                fotoLatariaBase64 = base64;
                binding.imgPreviewLataria.setVisibility(View.VISIBLE);
                binding.imgPreviewLataria.setImageBitmap(bitmap);
                break;

            case REQ_FOTO_KILOMETRAGEM:
                fotokilometragemBase64 = base64;
                binding.imgPreviewKilometragem.setVisibility(View.VISIBLE);
                binding.imgPreviewKilometragem.setImageBitmap(bitmap);
                break;
        }
    }


    private String fileToBase64(String path) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap == null) {
                return null;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);

            bitmap.recycle();

            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
