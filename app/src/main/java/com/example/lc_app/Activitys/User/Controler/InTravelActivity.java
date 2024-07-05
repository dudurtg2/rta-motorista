package com.example.lc_app.Activitys.User.Controler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lc_app.Activitys.MainActivity;
import com.example.lc_app.Fuctions.DAO.Querys.QueryRTA;
import com.example.lc_app.Fuctions.DAO.View.AdapterViewRTA;
import com.example.lc_app.R;
import com.example.lc_app.databinding.ActivityInTravelBinding;
import com.example.lc_app.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class InTravelActivity extends AppCompatActivity {
    public ActivityInTravelBinding binding;
    private DocumentReference docRef, docRefRTA;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_in_travel);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        binding = ActivityInTravelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonList.setOnClickListener(v-> {
            IntentIntegrator integrator = new IntentIntegrator(InTravelActivity.this);
            integrator.setCaptureActivity(CaptureActivity.class);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });
        queryItems();
        getUser();
    }
    private void getUser() {
        firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) { binding.UserNameDisplay.setText("Motorista: " + documentSnapshot.getString("nome")); }
                    else { binding.UserNameDisplay.setText(mAuth.getCurrentUser().getDisplayName()); }
                })
                .addOnFailureListener(e ->  Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
    }
    private void queryItems() {
        QueryRTA queryRTATravel = new QueryRTA(this);
        queryRTATravel.readDataInTravel(dishesDTO -> {
            binding.listRTATravelview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            binding.listRTATravelview.setAdapter(new AdapterViewRTA(getApplicationContext(), dishesDTO));
        });
    }
    private void confirmDocExist(String uid) {
        if (mAuth.getCurrentUser() != null) {
            docRef = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    removeToTraver(uid);
                } else { Toast.makeText(this, "RTA não encontrado", Toast.LENGTH_SHORT).show(); }
            }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
        } else { Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show(); }
    }
    private void removeToTraver(String uid) {
        docRefRTA = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);
        docRefRTA.get().addOnSuccessListener(documentSnapshotRTA -> {
            if (documentSnapshotRTA.exists()) {
                String motorista = documentSnapshotRTA.getString("Motorista");
                String status = documentSnapshotRTA.getString("Status");
                if (motorista != null && motorista.equals(mAuth.getCurrentUser().getUid())) {
                    if (status != null && status.equals("em rota")) {
                        firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid())
                                .get().addOnSuccessListener(documentSnapshotUsuario -> {
                                    if (documentSnapshotUsuario.exists()) {
                                        deleteRTAFirebase(uid).addOnSuccessListener( aVoid -> firestore.collection("usuarios").document(mAuth.getCurrentUser().getUid())
                                                .update("RTA_sacas", FieldValue.arrayRemove(uid))
                                                .addOnSuccessListener(aVoid2 -> {
                                                            queryItems();
                                                            Toast.makeText(this, uid + " finalizada.", Toast.LENGTH_SHORT).show();
                                                        }
                                                ).addOnFailureListener(e -> Toast.makeText(this, "Erro ao remover RTA da rota: " + e.getMessage(), Toast.LENGTH_SHORT).show())
                                        ).addOnFailureListener(e -> Toast.makeText(this, "Erro ao atualizar status." + e.getMessage(), Toast.LENGTH_SHORT).show());

                                    }
                                });
                    } else { Toast.makeText(this, "RTA não está em rota", Toast.LENGTH_SHORT).show(); }
                } else { Toast.makeText(this, "Motorista não corresponde a RTA", Toast.LENGTH_SHORT).show(); }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());

    }

    private Task<DocumentSnapshot> deleteRTAFirebase(String uid) {
        docRefRTA = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);
        return docRefRTA.get().addOnSuccessListener(documentSnapshotRTA -> {
            if (documentSnapshotRTA.exists()) {
                docRefRTA.delete().addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "RTA " + uid + " removida.", Toast.LENGTH_SHORT).show();

                    Map<String, Object> finalizadoData = new HashMap<>();
                    finalizadoData.put(uid, new Timestamp(new Date()));

                    firestore.collection("finalizados").document(mAuth.getCurrentUser().getUid()).update(finalizadoData);

                }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao remover RTA: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) { Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();}
            else {confirmDocExist(result.getContents());}
        } else { super.onActivityResult(requestCode, resultCode, data);}
    }
}