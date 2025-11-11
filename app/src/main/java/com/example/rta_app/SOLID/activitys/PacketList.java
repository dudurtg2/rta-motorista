package com.example.rta_app.SOLID.activitys;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rta_app.R;
import com.example.rta_app.SOLID.Views.Packetlista.AdapterViewRTA;
import com.example.rta_app.SOLID.api.PackingRepository;
import com.example.rta_app.SOLID.entities.Packet;
import com.example.rta_app.databinding.ActivityPacketListBinding;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import java.util.List;

public class PacketList extends AppCompatActivity {
    private ActivityPacketListBinding binding;
    private PackingRepository packingRepository;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_packet_list);
        binding = ActivityPacketListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        this.packingRepository = new PackingRepository(this);
        queryItems();
        setupBinding();
    }
    private void setupBinding() {
        binding.RTAprocura.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed()) {

                    packingRepository.postPacked(binding.RTAprocura.getText().toString()).addOnSuccessListener(packingList -> {
                        alertaSubmit(true, binding.RTAprocura.getText().toString());
                        binding.RTAprocura.setText("");
                    }).addOnFailureListener(va -> {
                        alertaSubmit(false, binding.RTAprocura.getText().toString());
                        binding.RTAprocura.setText("");
                    });
                    return true;
                }
            }
            return false;
        });
        binding.buttonList.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(PacketList.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES); // Suporte a todos os tipos de códigos
            integrator.setCaptureActivity(CaptureActivity.class); // Define a Activity de captura
            integrator.setOrientationLocked(false); // Permite rotação
            integrator.setPrompt("Posicione o código na câmera"); // Mensagem na tela
            integrator.setBeepEnabled(true); // Habilita som ao escanear
            integrator.setBarcodeImageEnabled(true); // Salva imagem do código escaneado (opcional)
            integrator.initiateScan();
        });

        binding.atualizar.setOnClickListener(v -> queryItems());
    }
    public void queryItems() {
        packingRepository.getListPacking().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Packet> packets = task.getResult();
                if (packets.isEmpty()) {
                    packets.add(new Packet(
                            "Sem devolução",
                            "indisponível",
                            "indisponível",
                            "indisponível"
                    ));
                }
                binding.listPacketTravel.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                binding.listPacketTravel.setAdapter(new AdapterViewRTA(this, packets));
            } else {
                List<Packet> packets = task.getResult();
                if (packets.isEmpty()) {
                    packets.add(new Packet(
                            "Sem devolução",
                            "indisponível",
                            "indisponível",
                            "indisponível"
                    ));
                }

                binding.listPacketTravel.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                binding.listPacketTravel.setAdapter(new AdapterViewRTA(this, packets));
            }
        });
    }

    private void alertaSubmit(Boolean success, String scannedCode) {
        if (success) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirmação")
                    .setMessage("O pacote com o código " + scannedCode + " foi registrado para devolução.")
                    .setPositiveButton("Devolver à base", (dialog, which) -> {
                        queryItems();
                        Toast.makeText(this, "Adicionado à lista de devolução", Toast.LENGTH_SHORT).show();
                        closeOptionsMenu();
                    }).show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Alerta!")
                    .setMessage("O código " + scannedCode + " não foi encontrado. Por favor, tente novamente.")
                    .setPositiveButton("Ok", (dialog, which) -> {
                        closeOptionsMenu();
                    }).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        binding.progressBar2.setVisibility(View.VISIBLE);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Operação cancelada", Toast.LENGTH_LONG).show();
            } else {
                String scannedCode = result.getContents();
                packingRepository.postPacked(scannedCode).addOnSuccessListener(packingList -> {
                    binding.progressBar2.setVisibility(View.GONE);
                    alertaSubmit(true, scannedCode);
                }).addOnFailureListener(v -> {
                        binding.progressBar2.setVisibility(View.GONE);
                        alertaSubmit(false, scannedCode);}
                );
            }


        } else {
            binding.progressBar2.setVisibility(View.GONE);
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}