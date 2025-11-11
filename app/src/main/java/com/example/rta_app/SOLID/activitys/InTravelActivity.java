package com.example.rta_app.SOLID.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rta_app.SOLID.Views.RTAlista.AdapterViewRTA;
import com.example.rta_app.SOLID.entities.PackingList;
import com.example.rta_app.SOLID.api.PackingListRepository;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.databinding.ActivityInTravelBinding;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InTravelActivity extends AppCompatActivity {

    public ActivityInTravelBinding binding;
    private String filter = "Todas as cidades";
    private UsersRepository usersRepository;
    private PackingListRepository packingListRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityInTravelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        this.packingListRepository = new PackingListRepository(this);
        this.usersRepository = new UsersRepository(this);
        setupBinding();
        queryItems(filter);
        queryFilter();

    }

    private void setupBinding() {
        binding.buttonList.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(InTravelActivity.this);
            integrator.setCaptureActivity(CaptureActivity.class);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        binding.atualizar.setOnClickListener(v -> queryItems(filter));

        binding.RTAprocura.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                if (event == null || !event.isShiftPressed()) {
                    String searchText = binding.RTAprocura.getText().toString().toUpperCase();

                    packingListRepository.getPackingListToRota(searchText).addOnSuccessListener(packingList -> {
                        if (packingList != null) {
                            String funcionario = packingList.getFuncionario();
                            if (funcionario != null && !funcionario.isEmpty()) {
                                Toast.makeText(this, funcionario, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Funcionário não disponível", Toast.LENGTH_SHORT).show();
                            }

                            Intent intent = new Intent(this, RTADetailsActivity.class);
                            intent.putExtra("uid", packingList.getCodigodeficha());
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            this.startActivity(intent);
                        } else {
                            Toast.makeText(this, "Romaneio não encontrado", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(va ->
                            Toast.makeText(this, searchText + " não encontrado", Toast.LENGTH_SHORT).show()
                    );

                    return true;
                }
            }
            return false;
        });


        binding.DishesCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filter = (String) parent.getItemAtPosition(position);
                queryItems(filter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    private void queryFilter() {
        packingListRepository.getListPackingListBase().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<PackingList> packingList = task.getResult();
                Set<String> packingLocalSet = new HashSet<>();
                List<String> packingLocal = new ArrayList<>();

                packingLocal.add("Todas as cidades");

                if (packingList != null) {
                    for (PackingList p : packingList) {
                        if (p != null && p.getLocal() != null) {
                            packingLocalSet.add(p.getLocal());
                        }
                    }
                    packingLocal.addAll(packingLocalSet);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(InTravelActivity.this, android.R.layout.simple_spinner_dropdown_item, packingLocal);
                binding.DishesCategorySpinner.setAdapter(adapter);
            } else {
                Log.d("Firestore", "Erro ao obter PackingList: " + task.getException().getMessage());
            }
        });
    }

    private void queryItems(String filter) {
        packingListRepository.getListPackingListBase().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<PackingList> packingList = task.getResult();
                List<PackingList> filteredList = new ArrayList<>();

                if (packingList != null) {
                    for (PackingList packing : packingList) {
                        if (packing != null && (filter.equals("Todas as cidades")
                                || (packing.getLocal() != null && packing.getLocal().equals(filter)))) {
                            filteredList.add(packing);
                        }
                    }
                }
                int itemCount = filteredList.size();
                binding.QtdRTA.setText(""+itemCount);

                binding.listRTATravelview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                binding.listRTATravelview.setAdapter(new AdapterViewRTA(1, getApplicationContext(), filteredList));
            } else {
                Log.d("Firestore", "Erro ao obter a lista de PackingList: " + task.getException().getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelado", Toast.LENGTH_LONG).show();
            } else {
                String scannedCode = result.getContents();
                packingListRepository.getPackingListToRota(scannedCode).addOnSuccessListener(packingList -> {
                    if (packingList != null) {
                        String codigodeficha = packingList.getCodigodeficha();
                        if (codigodeficha != null && !codigodeficha.isEmpty()) {
                            Toast.makeText(this, codigodeficha, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, RTADetailsActivity.class);
                            intent.putExtra("uid", codigodeficha);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Código de ficha não disponível", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, scannedCode + " não encontrado", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(va ->
                        Toast.makeText(this, scannedCode + " não encontrado", Toast.LENGTH_SHORT).show()
                );
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
