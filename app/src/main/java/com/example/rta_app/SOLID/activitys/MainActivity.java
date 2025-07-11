package com.example.rta_app.SOLID.activitys;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rta_app.SOLID.Views.Coletalista.AdapterViewRTA;
import com.example.rta_app.SOLID.api.PackingRepository;
import com.example.rta_app.SOLID.entities.Coletas;
import com.example.rta_app.SOLID.api.PackingListRepository;
import com.example.rta_app.SOLID.api.UsersRepository;
import com.example.rta_app.R;
import com.example.rta_app.databinding.ActivityMainBinding;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public ActivityMainBinding binding;
    private UsersRepository usersRepository;
    private PackingListRepository packingListRepository;
    private PackingRepository packingRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        this.packingListRepository = new PackingListRepository(this);
        this.usersRepository = new UsersRepository(this);
        this.packingRepository = new PackingRepository(this);
        getUser();
        SetupBinding();
        queryItems();

    }


    private void getUser() {
        usersRepository.getUser()
                .addOnSuccessListener(users -> {
                    binding.UserNameDisplay.setText(users.getName());
                    binding.telefone.setText(users.getTelefone().replaceAll("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3"));
                    binding.base.setText(users.getBase());


                    if (users.isFrete()) {
                        binding.buttonWorkHour.setOnClickListener(v -> startActivity(new Intent(this, WorkHourActivity.class)));
                        binding.textView.setOnClickListener(v -> startActivity(new Intent(this, WorkHourActivity.class)));
                    } else {
                        binding.buttonWorkHour.setOnClickListener(v -> Toast.makeText(this, "Essa opção não está disponível para você", Toast.LENGTH_SHORT));
                        binding.textView.setOnClickListener(v -> Toast.makeText(this, "Essa opção não está disponível para você", Toast.LENGTH_SHORT));
                    }
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
        binding.textView5.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setCaptureActivity(CaptureActivity.class);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        binding.RTAprocura.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed()) {
                    packingListRepository.getPackingListToBase(binding.RTAprocura.getText().toString().toUpperCase()).addOnSuccessListener(packingList -> {
                        if (packingList != null) {
                            String codigodeficha = packingList.getCodigodeficha();
                            if (codigodeficha != null && !codigodeficha.isEmpty()) {
                                new AlertDialog.Builder(this)
                                        .setTitle("Confirmação")
                                        .setMessage("Deseja adicionar a ficha " + packingList.getCodigodeficha() + "?" + "\nCidade: " + packingList.getLocal() + "\nData: " + packingList.getHoraedia().replace("T", " ").split("\\.")[0])
                                        .setPositiveButton("Coletar", (dialog, which) -> packingListRepository.movePackingListForDelivery(packingList).addOnSuccessListener(vda -> {
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


                        binding.RTAprocura.setText("");

                    }).addOnFailureListener(va -> {

                        binding.RTAprocura.setText("");
                    });
                    return true;
                }
            }
            return false;
        });

        binding.Perfil.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        binding.inTravelbutton.setOnClickListener(v -> startActivity(new Intent(this, InTravelActivity.class)));
        binding.textView2.setOnClickListener(v -> startActivity(new Intent(this, InTravelActivity.class)));
        binding.PackectList.setOnClickListener(v -> startActivity(new Intent(this, PacketList.class)));
        binding.textView4.setOnClickListener(v -> startActivity(new Intent(this, PacketList.class)));
        binding.atualizar.setOnClickListener(v ->

                queryItems());
    }

    public void queryItems() {
        packingRepository.colectPack().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Coletas> coletas = task.getResult();
                if (coletas.isEmpty()) {
                    coletas.add(new Coletas(
                            "Verifique com a base ou com o entregador",
                            "Sem devolução",
                            "0"

                    ));
                }
                binding.listPacketTravelDevo.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                binding.listPacketTravelDevo.setAdapter(new AdapterViewRTA(this, coletas));
            } else {
                List<Coletas> coletas = task.getResult();
                if (coletas.isEmpty()) {
                    coletas.add(new Coletas(
                            "Verifique com a base ou com o entregador",
                            "Sem devolução",
                            "0"

                    ));
                }

                binding.listPacketTravelDevo.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                binding.listPacketTravelDevo.setAdapter(new AdapterViewRTA(this, coletas));
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
                                    .setMessage("Deseja adicionar a ficha " + packingList.getCodigodeficha() + "?" + "\nCidade: " + packingList.getLocal() + "\nData: " + packingList.getHoraedia().replace("T", " ").split("\\.")[0])
                                    .setPositiveButton("Coletar", (dialog, which) -> packingListRepository.movePackingListForDelivery(packingList).addOnSuccessListener(vda -> {
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
                }).addOnFailureListener(va -> {
                    packingListRepository.getPackingListToDirect(scannedCode).addOnSuccessListener(packingList -> {
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
