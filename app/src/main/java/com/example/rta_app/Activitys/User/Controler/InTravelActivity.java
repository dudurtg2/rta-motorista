package com.example.rta_app.Activitys.User.Controler;

import android.content.Intent;
import android.os.Bundle;
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

import com.example.rta_app.Fuctions.DAO.Querys.QueryRTA;
import com.example.rta_app.Fuctions.DAO.View.AdapterViewRTA;
import com.example.rta_app.Fuctions.DTO.ListRTADTO;
import com.example.rta_app.R;
import com.example.rta_app.databinding.ActivityInTravelBinding;
import com.example.rta_app.Fuctions.DTO.ListRTADTO;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InTravelActivity extends AppCompatActivity {
    public ActivityInTravelBinding binding;
    private DocumentReference docRef, docRefRTA;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private boolean ocorrerncias;
    private List<String> listRTA;
    private String filter = "Todas as cidades";;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_in_travel);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        ocorrerncias = false;
        binding = ActivityInTravelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        listRTA = new ArrayList<>();

        binding.buttonList.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(InTravelActivity.this);
            integrator.setCaptureActivity(CaptureActivity.class);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });
        binding.atualizar.setOnClickListener(v -> queryItems(filter));
        binding.buttonFinaliza.setOnClickListener(v -> { removeFromTraver(); });
        binding.RTAprocura.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed()) {
                    confirmDocExist(binding.RTAprocura.getText().toString().toUpperCase());
                    return true;
                }
            }
            return false;
        });
        queryItems(filter);
        queryFilter();
        getUser();
    }

    private void removeFromTraver() {
        ocorrerncias = false;
        firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                if (documents.isEmpty()) {
                    Toast.makeText(this, "Nenhum pacote encontrado", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<Task<Void>> removalTasks = new ArrayList<>();

                for (DocumentSnapshot document : documents) {
                    String codigoDeFicha = document.getString("Codigo_de_ficha");
                    String status = document.getString("Status");
                    if (status.equals("Finalizado")) {

                        Task<Void> removalTask = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(codigoDeFicha).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Map<String, Object> finalizadoData = new HashMap<>();
                                    finalizadoData.put(codigoDeFicha, new Timestamp(new Date()));
                                    firestore.collection("finalizados").document(mAuth.getCurrentUser().getUid()).update(finalizadoData);
                                })
                                .addOnFailureListener(e -> {
                                    ocorrerncias = true;
                                    Toast.makeText(this, "Erro ao remover RTA: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });

                        removalTasks.add(removalTask);
                    } else {
                        ocorrerncias = true;
                    }
                }

                Tasks.whenAllComplete(removalTasks).addOnCompleteListener(completeTask -> {
                    if (ocorrerncias) {
                        Toast.makeText(this, "Rota contém pendências", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Rota finalizada", Toast.LENGTH_SHORT).show();
                    }
                    queryItems(filter);
                });

            } else {
                Toast.makeText(this, "Erro ao obter pacotes: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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

    public void readDataLocate(final FirestoreCallback firestoreCallback) {
        firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                listRTA.clear();
                listRTA.add("Todas as cidades");
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String city = document.getString("Local");

                    if (!listRTA.contains(city)) { listRTA.add(city); }
                }

                if (listRTA.isEmpty()) {  listRTA.add("Nada"); }
                Collections.sort(listRTA.subList(1, listRTA.size()));

                if (firestoreCallback != null) { firestoreCallback.onCallback(listRTA);}
            }
        });
    }

    public interface FirestoreCallback {
        void onCallback(List<String> listRTA);
    }

    private void queryFilter() {
        readDataLocate(listRTA -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(InTravelActivity.this, android.R.layout.simple_spinner_dropdown_item, listRTA);
            binding.DishesCategorySpinner.setAdapter(adapter);
        });
        binding.DishesCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filter = (String) parent.getItemAtPosition(position);
                queryItems((String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void queryItems(String filter) {
        QueryRTA queryRTATravel = new QueryRTA(this);
        queryRTATravel.readDataInTravel(dishesDTO -> {
            int itemCount = dishesDTO.size();

            binding.QtdRTA.setText(String.valueOf(itemCount));

            binding.listRTATravelview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            binding.listRTATravelview.setAdapter(new AdapterViewRTA(1, getApplicationContext(), dishesDTO));
        }, filter);
    }


    private void confirmDocExist(String uid) {
        if (mAuth.getCurrentUser() != null) {
            docRef = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);
            docRef.get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String status = documentSnapshot.getString("Status");
                            if (status.equals("Finalizado")) {
                                Toast.makeText(this, "RTA finalizada", Toast.LENGTH_SHORT).show();
                            } else {
                                Intent intent = new Intent(this, RTADetailsActivity.class);
                                intent.putExtra("uid", uid);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                this.startActivity(intent);
                                finish();
                            }
                        } else {
                            Toast.makeText(this, "RTA não encontrado", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
        }
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
}