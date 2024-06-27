package com.example.lc_app.Activitys.User.Controler;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.lc_app.Fuctions.DAO.Controler.ImageUploaderDAO;
import com.example.lc_app.Fuctions.DAO.User.UserDAO;
import com.example.lc_app.R;
import com.example.lc_app.databinding.ActivityWorkHourBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WorkHourActivity extends AppCompatActivity {

    private DocumentReference docRef, docHour;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private ImageUploaderDAO imageUploader;
    private Uri photoURI;
    public static final int PICK_IMAGE_REQUEST = 1;
    public static final int REQUEST_IMAGE_CAPTURE = 2;
    public ActivityWorkHourBinding binding;
    private String hourType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_hour);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            docRef = firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid());
            docHour = firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid()).collection("work_hours").document(new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
        } else {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding = ActivityWorkHourBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
        getUser();
    }
    @Override
    protected void onStart() {
        super.onStart();
        new ImageUploaderDAO(this, "Entrada").loadImagem();
        new ImageUploaderDAO(this, "Almoço").loadImagem();
        new ImageUploaderDAO(this, "Saída").loadImagem();
        new ImageUploaderDAO(this, "Fim").loadImagem();
        loadInitialData();
    }
    private void getUser() {
        firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        binding.UserNameDisplay.setText(documentSnapshot.getString("nome"));
                    } else {
                        binding.UserNameDisplay.setText(mAuth.getCurrentUser().getDisplayName());
                    }
                })
                .addOnFailureListener(e -> { Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show(); });
    }
    private void validateFields(String hour, OnValidationCompleteListener listener) {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        docHour = firestore.collection("usuarios")
                .document(mAuth.getCurrentUser().getUid())
                .collection("work_hours")
                .document(currentDate);

        docHour.get().addOnSuccessListener(documentSnapshot -> {
            boolean fieldExists = false;
            if (documentSnapshot.exists()) {
                fieldExists = documentSnapshot.contains(hour);
            }
            listener.onComplete(!fieldExists);
        }).addOnFailureListener(e -> {
            listener.onComplete(true);
        });
    }
    interface OnValidationCompleteListener {
        void onComplete(boolean isValid);
    }

    private void setupClickListeners() {
        binding.imageFistHour.setOnClickListener(v ->
            validateFields("Entrada", isValid -> {
                if (isValid) {
                    openCamera("Entrada");
                    hourType = "Entrada";
                } else { Toast.makeText(this, "O horário de entrada já foi registrado", Toast.LENGTH_SHORT).show();}
            })
        );
        binding.imageDinnerStarHour.setOnClickListener(v ->
                validateFields("Almoço", isValid -> {
                    if (isValid) {
                        openCamera("Almoço");
                        hourType = "Almoço";
                    } else { Toast.makeText(this, "O horário de Almoço já foi registrado", Toast.LENGTH_SHORT).show();}
                })
        );
        binding.imageDinnerFinishHour.setOnClickListener(v ->
                validateFields("Saída", isValid -> {
                    if (isValid) {
                        openCamera("Saída");
                        hourType = "Saída";
                    } else { Toast.makeText(this, "O horário de Saída do Almoço já foi registrado", Toast.LENGTH_SHORT).show();}
                })
        );
        binding.imageStop.setOnClickListener(v ->
                validateFields("Fim", isValid -> {
                    if (isValid) {
                        openCamera("Fim");
                        hourType = "Fim";
                    } else { Toast.makeText(this, "O horário de Fim de expediente já foi registrado", Toast.LENGTH_SHORT).show();}
                })
        );
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
                photoURI = FileProvider.getUriForFile(this, "com.example.lc_app.provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    private void updateWorkHour(String hourType) {
        UpdateWorkHours(hourType).addOnSuccessListener(task ->
                docHour.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        switch (hourType) {
                            case "Entrada":
                                binding.buttonFistHour.setText(documentSnapshot.getString("Entrada"));
                                break;
                            case "Almoço":
                                binding.buttonDinnerStarHour.setText(documentSnapshot.getString("Almoço"));
                                break;
                            case "Saída":
                                binding.buttonDinnerFinishHour.setText(documentSnapshot.getString("Saída"));
                                break;
                            case "Fim":
                                binding.buttonStop.setText(documentSnapshot.getString("Fim"));
                                break;
                        }
                    }
                })
        );
    }

    private void loadInitialData() {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        docHour = firestore.collection("usuarios")
                .document(mAuth.getCurrentUser().getUid())
                .collection("work_hours")
                .document(currentDate);
        docHour.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("Fim")) {
                    binding.buttonStop.setText(documentSnapshot.getString("Fim"));
                } else {
                    binding.buttonStop.setText("Fim");
                }

                if (documentSnapshot.contains("Saída")) {
                    binding.buttonDinnerFinishHour.setText(documentSnapshot.getString("Saída"));
                } else {
                    binding.buttonDinnerFinishHour.setText("Saída");
                }

                if (documentSnapshot.contains("Almoço")) {
                    binding.buttonDinnerStarHour.setText(documentSnapshot.getString("Almoço"));
                } else {
                    binding.buttonDinnerStarHour.setText("Almoço");
                }

                if (documentSnapshot.contains("Entrada")) {
                    binding.buttonFistHour.setText(documentSnapshot.getString("Entrada"));
                } else {
                    binding.buttonFistHour.setText("Entrada");
                }
            } else {
                binding.buttonStop.setText("Fim");
                binding.buttonDinnerFinishHour.setText("Saída");
                binding.buttonDinnerStarHour.setText("Almoço");
                binding.buttonFistHour.setText("Entrada");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (imageUploader != null) {
                updateWorkHour(hourType);
                imageUploader.handleCameraResult(photoURI, this);
            }
        }
    }

    private Task<Void> UpdateWorkHours(String horario) {
        String dateString = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        String timeString = new SimpleDateFormat("HH:mm").format(new Date());

        return new UserDAO(this).updateWorkHours(mAuth.getCurrentUser().getUid(), dateString, timeString, horario);
    }
}
