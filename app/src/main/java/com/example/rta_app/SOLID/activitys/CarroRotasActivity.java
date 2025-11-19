package com.example.rta_app.SOLID.activitys;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.rta_app.databinding.ActivityCarroRotasBinding;

public class CarroRotasActivity extends AppCompatActivity {
    private ActivityCarroRotasBinding binding;
    private static final int REQ_FOTO_COMBUSTIVEL = 1;
    private static final int REQ_FOTO_PARABRISA = 2;
    private static final int REQ_FOTO_LATARIA = 3;
    private static final int REQ_CAMERA_PERMISSION = 100;
    private int pendingRequestCodeForCamera = -1;
    private String fotoCombustivelBase64;
    private String fotoParabrisaBase64;
    private String fotoLatariaBase64;
    private CheckBox[] mecanicaChecks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCarroRotasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupChecklistMecanico();
        setupFotoButtons();
    }

    private void setupChecklistMecanico() {
        mecanicaChecks = new CheckBox[]{
                binding.chkAgua,
                binding.chkOleoMotor,
                binding.chkOleoFreio,
                binding.chkOleoDirecao,
                binding.chkFarois,
                binding.chkPiscas,
                binding.chkLuzFreio,
                binding.chkLuzRe,
                binding.chkPneus,
                binding.chkStep,
                binding.chkChaveRodas,
                binding.chkMacaco
        };

        for (int i = 1; i < mecanicaChecks.length; i++) {
            mecanicaChecks[i].setVisibility(View.GONE);
        }

        binding.lblFotos.setVisibility(View.GONE);
        binding.layoutFotos.setVisibility(View.GONE);
        binding.btnFinalizar.setVisibility(View.GONE);

        for (int i = 0; i < mecanicaChecks.length - 1; i++) {
            final int index = i;
            CheckBox atual = mecanicaChecks[i];
            CheckBox proximo = mecanicaChecks[i + 1];

            atual.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    proximo.setVisibility(View.VISIBLE);
                } else {
                    esconderDoIndex(index + 1);
                }
            });
        }
        CheckBox ultimo = mecanicaChecks[mecanicaChecks.length - 1];
        ultimo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.lblFotos.setVisibility(View.VISIBLE);
                binding.layoutFotos.setVisibility(View.VISIBLE);
                binding.btnFinalizar.setVisibility(View.VISIBLE);
            } else {
                binding.lblFotos.setVisibility(View.GONE);
                binding.layoutFotos.setVisibility(View.GONE);
                binding.btnFinalizar.setVisibility(View.GONE);
            }
        });
    }

    private void esconderDoIndex(int index) {
        for (int i = index; i < mecanicaChecks.length; i++) {
            mecanicaChecks[i].setChecked(false);
            mecanicaChecks[i].setVisibility(View.GONE);
        }

        binding.lblFotos.setVisibility(View.GONE);
        binding.layoutFotos.setVisibility(View.GONE);
        binding.btnFinalizar.setVisibility(View.GONE);
    }

    private void setupFotoButtons() {
        binding.btnFotoCombustivel.setOnClickListener(v -> abrirCamera(REQ_FOTO_COMBUSTIVEL));
        binding.btnFotoParabrisa.setOnClickListener(v -> abrirCamera(REQ_FOTO_PARABRISA));
        binding.btnFotoLataria.setOnClickListener(v -> abrirCamera(REQ_FOTO_LATARIA));

        binding.btnFinalizar.setOnClickListener(v -> Toast.makeText(this, "Vistoria finalizada (simulado)", Toast.LENGTH_SHORT).show());
    }

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
            startActivityForResult(intent, requestCode);
        } else {
            Toast.makeText(this, "Não foi possível abrir a câmera", Toast.LENGTH_SHORT).show();
        }
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

        if (resultCode != Activity.RESULT_OK || data == null) return;

        Bundle extras = data.getExtras();
        if (extras == null) return;

        Bitmap bitmap = (Bitmap) extras.get("data");
        if (bitmap == null) return;

        String base64 = bitmapToBase64(bitmap);

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
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}

