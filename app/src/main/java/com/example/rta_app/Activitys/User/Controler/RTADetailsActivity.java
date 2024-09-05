package com.example.rta_app.Activitys.User.Controler;

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

import com.example.rta_app.Fuctions.DAO.Driver.ImageUploaderDAO;
import com.example.rta_app.R;
import com.example.rta_app.databinding.ActivityRtadetailsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RTADetailsActivity extends AppCompatActivity {
    private ActivityRtadetailsBinding binding;
    private DocumentReference docRef, docRefRTA;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String uid;
    private ImageUploaderDAO imageUploader;
    private Uri photoURI;
    public static final int PICK_IMAGE_REQUEST = 1;
    public static final int REQUEST_IMAGE_CAPTURE = 2;
    private String QA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rtadetails);
        binding = ActivityRtadetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        Intent intent = getIntent();
        if (intent != null) {
            uid = intent.getStringExtra("uid");
        }
        binding.buttonRecusar.setOnClickListener(v -> statusUpdate(uid, "Recusado"));
        binding.buttonFinalizar.setOnClickListener(v -> {
            QA = "Finalizado";
            openCamera(uid);
        });
        binding.buttonOcorrencia.setOnClickListener(v -> {
            if (!binding.multiAutoCompleteTextView.getText().toString().isEmpty()) {
                QA = "Ocorrencia";
                openCamera(uid);
            } else {
                Toast.makeText(this, "Preencha o campo de ocorrencia", Toast.LENGTH_SHORT).show();
            }
        });
        binding.textRTA.setOnClickListener(v -> downloadRTA(uid));
        getRTA(uid);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (imageUploader != null) {
                statusUpdate(uid, QA);
                imageUploader.handleCameraResult(photoURI, this);
            }
        }
    }

    private void openCamera(String hour) {
        imageUploader = new ImageUploaderDAO(this, hour);
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
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


    private void downloadRTA(String uid) {
        docRefRTA = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);

        docRefRTA.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String downloadLink = documentSnapshot.getString("Download_link");
                        if (downloadLink != null) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadLink));
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Download link not available", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Document does not exist", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> { Toast.makeText(this, "Failed to fetch document: " + e.getMessage(), Toast.LENGTH_SHORT).show(); });
    }

    private void statusUpdate(String uid, String status) {
        docRef = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);

        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("Status", status);

                        if (status.equals("Ocorrencia")) {
                            if (!binding.multiAutoCompleteTextView.getText().toString().isEmpty()) {
                                updateData.put("Ocorrencia", binding.multiAutoCompleteTextView.getText().toString());
                            } else {
                                Toast.makeText(this, "Preencha o campo de ocorrencia", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        docRef.update(updateData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Status atualizado para " + status, Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao atualizar status", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Documento não encontrado", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
    }

    private void getRTA(String uid) {
        docRef = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);
        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        binding.textRTA.setText(uid + " \uD83D\uDCBE");
                        binding.TextCidade.setText("Cidade: " + documentSnapshot.getString("Local"));
                        binding.textDate.setText("Data: " + documentSnapshot.getString("Hora_e_Dia"));
                        binding.textCount.setText(documentSnapshot.getString("Quantidade"));
                        binding.textEntregador.setText("Entregador: " + documentSnapshot.getString("Entregador"));
                        binding.Empresa.setText("Empresa: " + documentSnapshot.getString("Empresa"));

                        List<String> codes = (List<String>) documentSnapshot.get("Codigos inseridos");
                        binding.textAllCodes.setText("Codigos inseridos:\n");
                        if (codes != null) {
                            for (String code : codes) {
                                binding.textAllCodes.append(code + "\n");
                            }
                        }
                    } else {
                        binding.textRTA.setText("Document does not exist");
                        binding.TextCidade.setText("");
                        binding.textDate.setText("");
                        binding.textCount.setText("");
                        binding.textEntregador.setText("");
                        binding.textAllCodes.setText("");
                        binding.Empresa.setText("");
                    }
                })
                .addOnFailureListener(e -> { Toast.makeText(getApplicationContext(), "Error fetching document: " + e.getMessage(), Toast.LENGTH_SHORT).show(); });
    }
}