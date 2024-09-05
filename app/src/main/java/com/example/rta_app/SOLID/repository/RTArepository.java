package com.example.rta_app.SOLID.repository;

import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;


public class RTArepository {
    private DocumentReference docRef, docRefRTA;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private boolean ocorrerncias;
    private List<String> listRTA;

    public RTArepository(){
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
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


    private String getUser() {
        AtomicReference<String> userName = new AtomicReference<>("");
        firestore.collection("usuarios")
                .document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userName.set("\uD83D\uDE9B " + documentSnapshot.getString("nome"));
                    } else {
                        userName.set(mAuth.getCurrentUser().getDisplayName());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
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
}
