package com.example.rta_app.SOLID.repository;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.rta_app.SOLID.activitys.MainActivity;
import com.example.rta_app.SOLID.activitys.RTADetailsActivity;
import com.example.rta_app.SOLID.services.GoogleSheetsService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

public class RTArepository {
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private boolean ocorrerncias;
    private Context context;

    public RTArepository(Context context) {
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        this.context = context;
    }
    public void addToTraver(String uid, int p) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(context, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference docRefBipagem = firestore.collection("bipagem").document(uid);
        docRefBipagem.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        handleStatus(documentSnapshot.getString("Status"), docRefBipagem, uid, p);
                    } else {

                        DocumentReference docRefDirecionado = firestore.collection("direcionado")
                                .document(mAuth.getCurrentUser().getUid())
                                .collection("pacotes")
                                .document(uid);

                        docRefDirecionado.get()
                                .addOnSuccessListener(documentSnapshotDirecionado -> {
                                    if (documentSnapshotDirecionado.exists()) {
                                        handleStatus(documentSnapshotDirecionado.getString("Status"), docRefDirecionado, uid, p);
                                    } else {
                                        Toast.makeText(context, "RTA não encontrado", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(context, "Erro ao obter dados da coleção 'direcionado'", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Erro ao obter dados da coleção 'bipagem'", Toast.LENGTH_SHORT).show());
    }

    void handleStatus(String status, DocumentReference docRef, String uid, int p) {
        if (status == null) {
            Toast.makeText(context, "Status não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (status) {
            case "aguardando":
                if (p == 1) {
                    updateStatusAndMoveDocument(docRef, uid);
                } else {
                    showConfirmationDialog(docRef, uid);
                }
                break;

            case "Retirado":
                Toast.makeText(context, "Motorista já está em rota", Toast.LENGTH_SHORT).show();
                break;

            case "Finalizado":
                Toast.makeText(context, "Motorista já finalizou", Toast.LENGTH_SHORT).show();
                break;

            default:
                Toast.makeText(context, "Status desconhecido", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void updateStatusAndMoveDocument(DocumentReference docRefRTA, String uid) {
        docRefRTA.update("Motorista", mAuth.getCurrentUser().getUid())
                .addOnSuccessListener(aVoid -> docRefRTA.update("Status", "Retirado")
                        .addOnSuccessListener(aVoid2 -> {
                            moveDocumentToRotaFolder(uid);
                            Toast.makeText(context, uid + " adicionada a rota.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Erro ao adicionar RTA a rota. " + e.getMessage(), Toast.LENGTH_SHORT).show())
                )
                .addOnFailureListener(e -> Toast.makeText(context, "Erro ao adicionar RTA a rota. " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showConfirmationDialog(DocumentReference docRefRTA, String uid) {
        new AlertDialog.Builder(context)
                .setTitle("Confirmação")
                .setMessage("Você deseja realmente adicionar o RTA " + uid + " a rota?")
                .setPositiveButton("Sim", (dialog, which) -> updateStatusAndMoveDocument(docRefRTA, uid))
                .setNegativeButton("Não", null)
                .show();
    }

    private void moveDocumentToRotaFolder(String uid) {
        DocumentReference sourceDocRef = firestore.collection("bipagem").document(uid);
        DocumentReference targetDocRef = firestore.collection("rota")
                .document(mAuth.getCurrentUser().getUid())
                .collection("pacotes")
                .document(uid);

        sourceDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        Map < String, Object > docData = documentSnapshot.getData();
                        if (docData != null) {

                            targetDocRef.set(docData)
                                    .addOnSuccessListener(aVoid -> {

                                        sourceDocRef.delete()
                                                .addOnSuccessListener(aVoid1 -> {

                                                    if (context instanceof MainActivity) {
                                                        ((MainActivity) context).queryItems();
                                                    }
                                                })
                                                .addOnFailureListener(e -> Log.d("Firestore", "Erro ao deletar documento original: " + e.getMessage()));
                                    })
                                    .addOnFailureListener(e -> Log.d("Firestore", "Erro ao mover documento: " + e.getMessage()));
                        } else {
                            Log.d("Firestore", "Dados do documento de origem são nulos");
                        }
                    } else {

                        DocumentReference directedDocRef = firestore.collection("direcionado")
                                .document(mAuth.getCurrentUser().getUid())
                                .collection("pacotes")
                                .document(uid);

                        directedDocRef.get()
                                .addOnSuccessListener(directedDocSnapshot -> {
                                    if (directedDocSnapshot.exists()) {

                                        Map < String, Object > docData = directedDocSnapshot.getData();
                                        if (docData != null) {

                                            targetDocRef.set(docData)
                                                    .addOnSuccessListener(aVoid -> {

                                                        directedDocRef.delete()
                                                                .addOnSuccessListener(aVoid1 -> {
                                                                    if (context instanceof MainActivity) {
                                                                        ((MainActivity) context).queryItems();
                                                                    }
                                                                })
                                                                .addOnFailureListener(e -> Log.d("Firestore", "Erro ao deletar documento original: " + e.getMessage()));
                                                    })
                                                    .addOnFailureListener(e -> Log.d("Firestore", "Erro ao mover documento: " + e.getMessage()));
                                        } else {
                                            Log.d("Firestore", "Dados do documento de origem na coleção 'direcionado' são nulos");
                                        }
                                    } else {
                                        Log.d("Firestore", "Documento de origem não encontrado na coleção 'direcionado'");
                                    }
                                })
                                .addOnFailureListener(e -> Log.d("Firestore", "Erro ao obter documento de origem da coleção 'direcionado': " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> Log.d("Firestore", "Erro ao obter documento de origem: " + e.getMessage()));
    }

    public void readDataLocate(final FirestoreCallback firestoreCallback) {
        firestore.collection("rota")
                .document(mAuth.getCurrentUser().getUid())
                .collection("pacotes")
                .get()
                .addOnCompleteListener(task -> {
                    List < String > listRTA = new ArrayList < > ();
                    if (task.isSuccessful()) {
                        listRTA.add("Todas as cidades");
                        for (QueryDocumentSnapshot document: task.getResult()) {
                            String city = document.getString("Local");
                            if (!listRTA.contains(city)) {
                                listRTA.add(city);
                            }
                        }

                        if (listRTA.isEmpty()) {
                            listRTA.add("Nada");
                        }

                        Collections.sort(listRTA.subList(1, listRTA.size()));

                        if (firestoreCallback != null) {
                            firestoreCallback.onCallback(listRTA);
                        }
                    }
                });
    }

    public interface FirestoreCallback {
        void onCallback(List < String > listRTA);
    }

    public Task < List < Task < ? >>> removeFromTraver() {
        ocorrerncias = false;

        return firestore.collection("rota")
                .document(mAuth.getCurrentUser().getUid())
                .collection("pacotes")
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult().isEmpty()) {
                        Toast.makeText(context, "Nenhum pacote encontrado ou erro ao obter pacotes", Toast.LENGTH_SHORT).show();
                        return Tasks.forCanceled();
                    }

                    List < DocumentSnapshot > documents = task.getResult().getDocuments();
                    List < Task < Void >> removalTasks = new ArrayList < > ();

                    for (DocumentSnapshot document: documents) {
                        String codigoDeFicha = document.getString("Codigo_de_ficha");
                        String status = document.getString("Status");

                        if (codigoDeFicha == null || status == null) {
                            ocorrerncias = true;
                            continue; // Skip this document if data is missing
                        }

                        if (status.equals("Finalizado")) {
                            Task < Void > removalTask = firestore.collection("rota")
                                    .document(mAuth.getCurrentUser().getUid())
                                    .collection("pacotes")
                                    .document(codigoDeFicha)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Map < String,
                                                Object > finalizadoData = new HashMap < > ();
                                        finalizadoData.put(codigoDeFicha, new Timestamp(new Date()));
                                        firestore.collection("finalizados")
                                                .document(mAuth.getCurrentUser().getUid())
                                                .update(finalizadoData);
                                    })
                                    .addOnFailureListener(e -> {
                                        ocorrerncias = true;
                                        Toast.makeText(context, "Erro ao remover RTA: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });

                            removalTasks.add(removalTask);
                        } else {
                            ocorrerncias = true;
                        }
                    }

                    return Tasks.whenAllComplete(removalTasks)
                            .addOnCompleteListener(completeTask -> {
                                if (ocorrerncias) {
                                    Toast.makeText(context, "Rota contém pendências", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, "Rota finalizada", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    public void getUserName(OnUserNameCallback callback) {
        firestore.collection("usuarios")
                .document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String returnName;
                    if (documentSnapshot.exists()) {
                        returnName = documentSnapshot.getString("nome");
                    } else {
                        returnName = mAuth.getCurrentUser().getDisplayName();
                    }

                    callback.onUserNameReceived(returnName);
                })
                .addOnFailureListener(e -> {

                    callback.onUserNameReceived("Erro ao buscar nome");
                });
    }

    public interface OnUserNameCallback {
        void onUserNameReceived(String userName);
    }

    public void confirmDocExistMain(String uid) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(context, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
        DocumentReference docRef = firestore.collection("bipagem").document(uid);
        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        addToTraver(uid, 0);
                    } else {
                        DocumentReference directedDocRef = firestore.collection("direcionado")
                                .document(mAuth.getCurrentUser().getUid())
                                .collection("pacotes")
                                .document(uid);
                        directedDocRef.get()
                                .addOnSuccessListener(directedDocSnapshot -> {
                                    if (directedDocSnapshot.exists()) {
                                        addToTraver(uid, 1);
                                    } else {
                                        Toast.makeText(context, "RTA não" + uid + " encontrado", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(context, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
    }

    public void confirmDocExist(String uid) {
        if (mAuth.getCurrentUser() != null) {
            DocumentReference docRef = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);
            docRef.get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String status = documentSnapshot.getString("Status");
                            if (status != null && status.equals("Finalizado")) {
                                Toast.makeText(context, "RTA finalizada", Toast.LENGTH_SHORT).show();
                            } else {
                                Intent intent = new Intent(context, RTADetailsActivity.class);
                                intent.putExtra("uid", uid);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                                if (context instanceof Activity) {
                                    ((Activity) context).finish();
                                }
                            }
                        } else {
                            Toast.makeText(context, "RTA não encontrado", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(context, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
        }
    }
    public Task<Void> addUser(String name, String uid) {
        Map<String, Object> user = new HashMap<>();
        user.put("nome", name);
        user.put("uid", uid);

        return firestore.collection("usuarios").document(uid).update(user);
    }
    public Task<Void> addFinal(String rota, String uid) {
        Map<String, Object> user = new HashMap<>();
        user.put("nome", rota);
        user.put("uid", uid);

        return firestore.collection("finalizados").document(uid).update(user);
    }





}