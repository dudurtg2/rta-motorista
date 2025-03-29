package com.example.rta_app.SOLID.activitys;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.rta_app.SOLID.Interfaces.IPackingListRepository;
import com.example.rta_app.SOLID.entities.PackingList;
import com.example.rta_app.SOLID.api.PackingListRepository;
import com.example.rta_app.SOLID.services.ImageDriverService;
import com.example.rta_app.R;
import com.example.rta_app.databinding.ActivityRtadetailsBinding;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class RTADetailsActivity extends AppCompatActivity {

    private ActivityRtadetailsBinding binding;

    PackingList uid2;
    private String uii;
    private ImageDriverService imageUploader;
    private Uri photoURI;
    public static final int PICK_IMAGE_REQUEST = 1;
    public static final int REQUEST_IMAGE_CAPTURE = 2;
    private String QA;
    private PackingListRepository packingListRepository;

    public RTADetailsActivity() {
        this.packingListRepository = new PackingListRepository(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rtadetails);
        binding = ActivityRtadetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        if (intent != null) {
            uii = intent.getStringExtra("uid");
        }

        packingListRepository.getPackingListToRota(uii).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                PackingList uid = task.getResult();
                uid2 = uid;
                SetupClickListeners(uid);
                getRTA(uid);
            } else {
                Toast.makeText(this, "Falha ao obter Packing List", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void SetupClickListeners(PackingList uid) {
        binding.buttonRecusar.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Confirmação")
                .setMessage("O entregador realmente não vai receber a saca de código " + uid.getCodigodeficha() + "?")
                .setPositiveButton("Não vai receber", (dialog, which) -> statusUpdate(uid, "recusado"))
                .setNegativeButton("Vai receber", null)
                .show());

        binding.buttonFinalizar.setOnClickListener(v -> {
            QA = "finalizado";
            openCamera(uid.getCodigodeficha());
        });
        binding.buttonOcorrencia.setOnClickListener(v -> {
            if (!binding.multiAutoCompleteTextView.getText().toString().isEmpty()) {
                QA = "ocorrencia";
                openCamera(uid.getCodigodeficha());
            } else {
                Toast.makeText(this, "Preencha o campo de ocorrencia", Toast.LENGTH_SHORT).show();
            }
        });
        binding.textRTA.setOnClickListener(v -> downloadRTA(uid));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (imageUploader != null) {
                imageUploader.handleCameraResult(photoURI, this);
                statusUpdate(uid2, QA);
                finish();
            }
        }
    }

    private void openCamera(String uid) {
        imageUploader = new ImageDriverService(this, uid);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Erro ao criar arquivo de imagem", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, "com.example.rta_app.provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir != null && !storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                throw new IOException("Failed to create directory");
            }
        }

        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void downloadRTA(PackingList uid) {

        String downloadLink = uid.getDownloadlink();
        if (downloadLink != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadLink));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Download link not available", Toast.LENGTH_SHORT).show();
        }

    }

    private void statusUpdate(PackingList documentSnapshot, String status) {

        if (!documentSnapshot.getCodigodeficha().equals("")) {
            String occurrence = "";

            if (status.equals("ocorrencia")) {
                if (!binding.multiAutoCompleteTextView.getText().toString().isEmpty()) {
                    occurrence = binding.multiAutoCompleteTextView.getText().toString();
                    Toast.makeText(this, "Ocorrência: " + occurrence, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Preencha o campo de ocorrência", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            packingListRepository.updateStatusPackingList(documentSnapshot, occurrence, status)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Status atualizado para " + status, Toast.LENGTH_SHORT).show();
                        finish();
                        
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Erro ao atualizar status", Toast.LENGTH_SHORT).show());

        } else {
            Toast.makeText(this, "Documento não encontrado", Toast.LENGTH_SHORT).show();
        }
    }

    private void getRTA(PackingList documentSnapshot) {

        if (!documentSnapshot.getCodigodeficha().equals("")) {
            binding.textRTA.setText(documentSnapshot.getCodigodeficha() + " \uD83D\uDCBE");
            binding.TextCidade.setText("Local: " + documentSnapshot.getLocal());
            binding.textDate.setText("Data: " + documentSnapshot.getHoraedia().replace("T", " ").split("\\.")[0]);
            binding.textCount.setText("Quantidade: " + documentSnapshot.getQuantidade());
            binding.textTelefone.setText("Telefone: " + documentSnapshot.getTelefone());
            binding.textEntregador.setText("Entregador: " + documentSnapshot.getEntregador());
            binding.Empresa.setText("Empresa: " + documentSnapshot.getEmpresa());
            binding.textEndereco.setText("Endereço: " + documentSnapshot.getEndereco());
            binding.textAllCodes.setText("Codigos inseridos:\n");
            if (documentSnapshot.getCodigosinseridos() != null) {
                for (String code : documentSnapshot.getCodigosinseridos()) {
                    binding.textAllCodes.append(code + "\n");
                }
            }
        } else {
            binding.textRTA.setText("Documento não encontrado");
            binding.TextCidade.setText("");
            binding.textDate.setText("");
            binding.textCount.setText("");
            binding.textEntregador.setText("");
            binding.textAllCodes.setText("");
            binding.Empresa.setText("");
        }

    }

}
