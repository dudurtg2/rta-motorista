package com.example.rta_app.SOLID.activitys;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rta_app.SOLID.Interfaces.IPackingListRepository;
import com.example.rta_app.SOLID.Interfaces.IUsersRepository;
import com.example.rta_app.SOLID.entities.PackingList;
import com.example.rta_app.SOLID.api.PackingListRepository;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.SOLID.Views.AdapterViewRTA;
import com.example.rta_app.R;
import com.example.rta_app.databinding.ActivityMainBinding;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public ActivityMainBinding binding;
    private IUsersRepository usersRepository;
    private IPackingListRepository packingListRepository;

    public MainActivity() {
        this.packingListRepository = new PackingListRepository(this);
        this.usersRepository = new UsersRepository(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getUser();
        SetupBinding();
        queryItems();
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

    private void SetupBinding() {
        binding.buttonList.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setCaptureActivity(CaptureActivity.class);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });
        binding.RTAprocura.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed()) {
                    packingListRepository.getPackingListToBase(binding.RTAprocura.getText().toString().toUpperCase()).addOnSuccessListener(packingList -> {

                        packingListRepository.movePackingListForDelivery(packingList).addOnSuccessListener(va -> queryItems());

                    }).addOnFailureListener(va  -> Toast.makeText(this, binding.RTAprocura.getText().toString().toUpperCase() + " não encontrado", Toast.LENGTH_SHORT).show());
                    return true;
                }
            }
            return false;
        });
        binding.UserNameDisplay.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        binding.buttonWorkHour.setOnClickListener(v -> startActivity(new Intent(this, WorkHourActivity.class)));
        binding.inTravelbutton.setOnClickListener(v -> startActivity(new Intent(this, InTravelActivity.class)));

        binding.atualizar.setOnClickListener(v -> queryItems());
    }

    public void queryItems() {
        packingListRepository.getListPackingListToDirect().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<PackingList> packingList = task.getResult();
                if(packingList.isEmpty()){
                    packingList.add(new PackingList(
                            "LC HUB-01",
                            "indisponível",
                            "indisponível",
                            "indisponível",
                            "Favor esperar para novos remessas",
                            "Sacas em espera",
                            "",
                            "a",
                            "Status indisponível",
                            "indisponível",
                            null,
                            "indisponível",
                            "indisponível"
                    ));
                }
                binding.listRTAview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                binding.listRTAview.setAdapter(new AdapterViewRTA(0, this, packingList));
            } else {
                List<PackingList> packingList = new ArrayList<>();

                    packingList.add(new PackingList(
                            "LC HUB-01",
                            "indisponível",
                            "indisponível",
                            "indisponível",
                            "Favor esperar para novos remessas",
                            "Sacas em espera",
                            "",
                            "a",
                            "Status indisponível",
                            "indisponível",
                            null,
                            "indisponível",
                            "indisponível"
                    ));

                binding.listRTAview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                binding.listRTAview.setAdapter(new AdapterViewRTA(0, this, packingList));
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
                String scannedCode = result.getContents().toUpperCase();
                packingListRepository.getPackingListToBase(scannedCode).addOnSuccessListener(packingList -> {
                    if (packingList != null) {
                        String codigodeficha = packingList.getCodigodeficha();
                        if (codigodeficha != null && !codigodeficha.isEmpty()) {
                            new AlertDialog.Builder(this)
                                    .setTitle("Confirmação")
                                    .setMessage("Deseja adicionar a ficha " + packingList.getCodigodeficha() + "?" + "\nCidade: "+ packingList.getLocal() + "\nData: "+ packingList.getHoraedia())
                                    .setPositiveButton("Coletar", (dialog, which) ->  packingListRepository.movePackingListForDelivery(packingList).addOnSuccessListener(vda -> {
                                        queryItems();
                                        Toast.makeText(this, codigodeficha + "\n adicionado a rota", Toast.LENGTH_SHORT).show();
                                    }))
                                    .setNegativeButton("Não coletar", null)
                                    .show();

                        } else {
                            Toast.makeText(this, "Código de ficha inválido", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Packing list não encontrado", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(va -> { packingListRepository.getPackingListToDirect(scannedCode).addOnSuccessListener(packingList -> {
                    if (packingList != null) {
                        String codigodeficha = packingList.getCodigodeficha();
                        if (codigodeficha != null && !codigodeficha.isEmpty()) {
                            Toast.makeText(this, codigodeficha + "\n recebido e adicionado a rota", Toast.LENGTH_SHORT).show();
                            packingListRepository.movePackingListForDelivery(packingList).addOnSuccessListener(vad -> queryItems());

                        } else {
                            Toast.makeText(this, "Código de ficha inválido", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Saca não encontrada", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(vaa -> {
                    String message = scannedCode + " não encontrado";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                });
            });
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }



}
