package com.example.rta_app.Activitys;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rta_app.Activitys.User.Controler.InTravelActivity;
import com.example.rta_app.Activitys.User.Controler.WorkHourActivity;
import com.example.rta_app.Activitys.User.ProfileActivity;
import com.example.rta_app.Fuctions.DAO.Querys.QueryRTA;
import com.example.rta_app.Fuctions.DAO.View.AdapterViewRTA;
import com.example.rta_app.R;
import com.example.rta_app.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public ActivityMainBinding binding;
    private DocumentReference docRef, docRefRTA;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String rota, rtaRota;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getUser();

        binding.buttonList.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setCaptureActivity(CaptureActivity.class);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });
        binding.RTAprocura.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed()) {
                    confirmDocExist(binding.RTAprocura.getText().toString().toUpperCase());
                    return true;
                }
            }
            return false;
        });
        binding.UserNameDisplay.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        binding.buttonWorkHour.setOnClickListener(v -> startActivity(new Intent(this, WorkHourActivity.class)));
        binding.inTravelbutton.setOnClickListener(v -> startActivity(new Intent(this, InTravelActivity.class)));

        binding.atualizar.setOnClickListener(v -> queryItems());
        queryItems();
    }

    private void queryItems() {
        QueryRTA queryRTA = new QueryRTA(this);
        queryRTA.readData(dishesDTO -> {
            binding.listRTAview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            binding.listRTAview.setAdapter(new AdapterViewRTA(0, this, dishesDTO));
        });
    }

    public void confirmDocExist(String uid) {
        if (mAuth.getCurrentUser() != null) {
            docRef = firestore.collection("bipagem").document(uid);
            docRef.get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            addToTraver(uid);
                        } else {
                            Toast.makeText(this, "RTA não encontrado", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    public void addToTraver(String uid) {
        docRefRTA = firestore.collection("bipagem").document(uid);
        docRefRTA.get()
                .addOnSuccessListener(documentSnapshotRTA -> {
                    if (documentSnapshotRTA.exists()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Confirmação")
                                .setMessage("Você deseja realmente adicionar o RTA"+ uid +" a rota?")
                                .setPositiveButton("Sim", (dialog, which) -> {if (documentSnapshotRTA.getString("Status").equals("aguardando")) {
                                                                                        docRefRTA.update("Motorista", mAuth.getCurrentUser().getUid())
                                                                                                .addOnSuccessListener(aVoid -> {
                                                                                                    docRefRTA.update("Status", "Retirado").addOnSuccessListener(aVoid2 -> {
                                                                                                        moveDocumentToRotaFolder(uid);
                                                                                                        Toast.makeText(this, uid + " adicionada a rota.", Toast.LENGTH_SHORT).show();
                                                                                                    });
                                                                                                })
                                                                                                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao adicionar RTA a rota. " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                                                                    } else if (documentSnapshotRTA.getString("Status").equals("Retirado")) {
                                                                                        Toast.makeText(this, "Motorista já está em rota", Toast.LENGTH_SHORT).show();
                                                                                    } else if (documentSnapshotRTA.getString("Status").equals("Finalizado")) {
                                                                                        Toast.makeText(this, "Motorista já finalizou", Toast.LENGTH_SHORT).show();
                                                                                    }})
                                .setNegativeButton("Não", null)
                                .show();

                    } else {
                        Toast.makeText(this, "RTA não encontrado", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
    }

    private void moveDocumentToRotaFolder(String uid) {
        DocumentReference sourceDocRef = firestore.collection("bipagem").document(uid);
        DocumentReference targetDocRef = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);

        sourceDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> docData = documentSnapshot.getData();
                        if (docData != null) {
                            targetDocRef.set(docData).addOnSuccessListener(aVoid -> {
                                sourceDocRef.delete().addOnSuccessListener(aVoid1 -> queryItems()).addOnFailureListener(e -> Log.d("Firestore", "Erro ao deletar documento original: " + e.getMessage()));
                            }).addOnFailureListener(e -> Log.d("Firestore", "Erro ao mover documento: " + e.getMessage()));
                        }
                    } else {
                        Log.d("Firestore", "Documento de origem não encontrado");
                    }
                })
                .addOnFailureListener(e -> Log.d("Firestore", "Erro ao obter documento de origem: " + e.getMessage()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelado", Toast.LENGTH_LONG).show();
            } else {
                confirmDocExist(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void getUser() {
        firestore.collection("usuarios")
                .document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        binding.UserNameDisplay.setText("\uD83D\uDE9B " + documentSnapshot.getString("nome"));
                    } else {
                        binding.UserNameDisplay.setText(mAuth.getCurrentUser().getDisplayName());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
    }
}
