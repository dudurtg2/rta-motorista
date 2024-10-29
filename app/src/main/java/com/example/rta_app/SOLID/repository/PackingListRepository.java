package com.example.rta_app.SOLID.repository;

import android.widget.Toast;

import com.example.rta_app.SOLID.Interfaces.IPackingListRepository;
import com.example.rta_app.SOLID.entities.PackingList;
import com.example.rta_app.SOLID.entities.Users;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
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

public class PackingListRepository implements IPackingListRepository {
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    public PackingListRepository() {
        this.mAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }



    public Task<Void> finishPackingList() {
        String userId = mAuth.getCurrentUser().getUid();

        return firestore.collection("rota")
                .document(userId)
                .collection("pacotes")
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    List<Task<Void>> tasks = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {

                        String status = document.getString("status");
                        if ("Finalizado".equals(status)) {

                            Map<String, Object> finalizadoData = new HashMap<>();
                            finalizadoData.put(document.getId(), new Timestamp(new Date()));

                            Task<Void> updateTask = firestore.collection("finalizados")
                                    .document(userId)
                                    .update(finalizadoData);


                            tasks.add(updateTask);


                            Task<Void> deleteTask = document.getReference().delete();

                            tasks.add(deleteTask);
                        }
                    }


                    return Tasks.whenAll(tasks);
                });
    }



    public Task<Void> updateStatusPackingList(PackingList packingList, String ocorrencia, String status) {
        DocumentReference docRef = firestore.collection("rota")
                .document(mAuth.getCurrentUser().getUid())
                .collection("pacotes")
                .document(packingList.getCodigodeficha());

        Map<String, Object> packingLists = new HashMap<>();

        if ("Ocorrencia".equals(status) && ocorrencia != null && !ocorrencia.isEmpty()) {
            packingLists.put("ocorrencia", ocorrencia);
        }
        packingLists.put("status", status);
        packingLists.put("horaedia", packingList.getHoraedia());
        packingLists.put("codigodeficha", packingList.getCodigodeficha());
        packingLists.put("local", packingList.getLocal());
        packingLists.put("empresa", packingList.getEmpresa());
        packingLists.put("funcionario", packingList.getFuncionario());
        packingLists.put("entregador", mAuth.getCurrentUser().getUid());
        packingLists.put("motorista", packingList.getMotorista());
        packingLists.put("telefone", packingList.getTelefone());
        packingLists.put("quantidade", packingList.getQuantidade());
        packingLists.put("downloadlink", packingList.getDownloadlink());
        packingLists.put("codigosinseridos", packingList.getCodigosinseridos());

        return docRef.set(packingLists);
    }


    public Task<PackingList> getPackingListToDirect(String uid) {
        Task<DocumentSnapshot> task = firestore.collection("direcionado")
                .document(mAuth.getCurrentUser().getUid())
                .collection("pacotes")
                .document(uid)
                .get();

        return task.continueWith(taskSnapshot -> {
            if (!taskSnapshot.isSuccessful()) {
                throw taskSnapshot.getException();
            }

            DocumentSnapshot document = taskSnapshot.getResult();

            if (document == null || !document.exists()) {
            return new PackingList(null, null, null, null, null, null, null, null, null, null, null, null);
            }

            String Codigo_de_ficha = document.getString("codigodeficha");
            List<String> Codigos_inseridos = (List<String>) document.get("codigosinseridos");
            String Download_link = document.getString("downloadlink");
            String Empresa = document.getString("empresa");
            String Entregador = document.getString("entregador");
            String Funcionario = document.getString("funcionario");
            String Hora_e_Dia = document.getString("horaedia");
            String Local = document.getString("local") ;
            String Motorista = document.getString("motorista");
            String Quantidade = document.getString("quantidade");
            String Status = document.getString("status");
            String Telefone = document.getString("telefone");

            return new PackingList(
                    Empresa,
                    Funcionario,
                    Entregador,
                    Telefone,
                    Local,
                    Codigo_de_ficha,
                    Hora_e_Dia,
                    Quantidade,
                    Status,
                    Motorista,
                    Codigos_inseridos,
                    Download_link
            );

        });
    }

    public Task<PackingList> getPackingListToRota(String uid) {
        Task<DocumentSnapshot> task = firestore.collection("rota")
                .document(mAuth.getCurrentUser().getUid())
                .collection("pacotes")
                .document(uid)
                .get();

        return task.continueWith(taskSnapshot -> {
            if (!taskSnapshot.isSuccessful()) {
                throw taskSnapshot.getException();
            }

            DocumentSnapshot document = taskSnapshot.getResult();

            if (document == null || !document.exists()) {
                return new PackingList(null, null, null, null, null, null, null, null, null, null, null, null);
            }

            String Codigo_de_ficha = document.getString("codigodeficha");
            List<String> Codigos_inseridos = (List<String>) document.get("codigosinseridos");
            String Download_link = document.getString("downloadlink");
            String Empresa = document.getString("empresa");
            String Entregador = document.getString("entregador");
            String Funcionario = document.getString("funcionario");
            String Hora_e_Dia = document.getString("horaedia");
            String Local = document.getString("local") ;
            String Motorista = document.getString("motorista");
            String Quantidade = document.getString("quantidade");
            String Status = document.getString("status");
            String Telefone = document.getString("telefone");

            return new PackingList(
                    Empresa,
                    Funcionario,
                    Entregador,
                    Telefone,
                    Local,
                    Codigo_de_ficha,
                    Hora_e_Dia,
                    Quantidade,
                    Status,
                    Motorista,
                    Codigos_inseridos,
                    Download_link
            );

        });
    }

    public Task<PackingList> getPackingListToBase(String uid) {
        TaskCompletionSource<PackingList> taskCompletionSource = new TaskCompletionSource<>();

        firestore.collection("bipagem")
                .document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();

                        String Codigo_de_ficha = document.getString("codigodeficha");
                        List<String> Codigos_inseridos = (List<String>) document.get("codigosinseridos");
                        String Download_link = document.getString("downloadlink");
                        String Empresa = document.getString("empresa");
                        String Entregador = document.getString("entregador");
                        String Funcionario = document.getString("funcionario");
                        String Hora_e_Dia = document.getString("horaedia");
                        String Local = document.getString("local") ;
                        String Motorista = document.getString("motorista");
                        String Quantidade = document.getString("quantidade");
                        String Status = document.getString("status");
                        String Telefone = document.getString("telefone");

                        PackingList packingList = new PackingList(
                                Empresa,
                                Funcionario,
                                Entregador,
                                Telefone,
                                Local,
                                Codigo_de_ficha,
                                Hora_e_Dia,
                                Quantidade,
                                Status,
                                Motorista,
                                Codigos_inseridos,
                                Download_link
                        );

                        taskCompletionSource.setResult(packingList);
                    } else {
                        taskCompletionSource.setException(task.getException());
                    }
                });

        return taskCompletionSource.getTask();
    }

    public Task<List<PackingList>> getListPackingListToDirect() {
        TaskCompletionSource<List<PackingList>> taskCompletionSource = new TaskCompletionSource<>();

        firestore.collection("direcionado")
                .document(mAuth.getCurrentUser().getUid())
                .collection("pacotes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<PackingList> list = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String Codigo_de_ficha = document.getString("codigodeficha");
                            List<String> Codigos_inseridos = (List<String>) document.get("codigosinseridos");
                            String Download_link = document.getString("downloadlink");
                            String Empresa = document.getString("empresa");
                            String Entregador = document.getString("entregador");
                            String Funcionario = document.getString("funcionario");
                            String Hora_e_Dia = document.getString("horaedia");
                            String Local = document.getString("local") ;
                            String Motorista = document.getString("motorista");
                            String Quantidade = document.getString("quantidade");
                            String Status = document.getString("status");
                            String Telefone = document.getString("telefone");

                            PackingList packingList = new PackingList(
                                    Empresa,
                                    Funcionario,
                                    Entregador,
                                    Telefone,
                                    Local,
                                    Codigo_de_ficha,
                                    Hora_e_Dia,
                                    Quantidade,
                                    Status,
                                    Motorista,
                                    Codigos_inseridos,
                                    Download_link
                            );
                            list.add(packingList);
                        }
                        taskCompletionSource.setResult(list);
                    } else {
                        taskCompletionSource.setException(task.getException());
                    }
                });

        return taskCompletionSource.getTask();
    }

    public Task<List<PackingList>> getListPackingListBase() {
        TaskCompletionSource<List<PackingList>> taskCompletionSource = new TaskCompletionSource<>();

        firestore.collection("rota")
                .document(mAuth.getCurrentUser().getUid())
                .collection("pacotes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<PackingList> list = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String Codigo_de_ficha = document.getString("codigodeficha");
                            List<String> Codigos_inseridos = (List<String>) document.get("codigosinseridos");
                            String Download_link = document.getString("downloadlink");
                            String Empresa = document.getString("empresa");
                            String Entregador = document.getString("entregador");
                            String Funcionario = document.getString("funcionario");
                            String Hora_e_Dia = document.getString("horaedia");
                            String Local = document.getString("local") ;
                            String Motorista = document.getString("motorista");
                            String Quantidade = document.getString("quantidade");
                            String Status = document.getString("status");
                            String Telefone = document.getString("telefone");

                            PackingList packingList = new PackingList(
                                    Empresa,
                                    Funcionario,
                                    Entregador,
                                    Telefone,
                                    Local,
                                    Codigo_de_ficha,
                                    Hora_e_Dia,
                                    Quantidade,
                                    Status,
                                    Motorista,
                                    Codigos_inseridos,
                                    Download_link
                            );
                            list.add(packingList);
                        }
                        taskCompletionSource.setResult(list);
                    } else {
                        taskCompletionSource.setException(task.getException());
                    }
                });

        return taskCompletionSource.getTask();
    }

    public Task<Void> movePackingListForDelivery(PackingList packingList) {
        if (packingList.getStatus().equals("aguardando")) {
             firestore.collection("direcionado")
                    .document(mAuth.getCurrentUser().getUid())
                    .collection("pacotes")
                    .document(packingList.getCodigodeficha()).get().addOnSuccessListener(value -> {
                        if (value.exists()) {
                            firestore.collection("direcionado")
                                    .document(mAuth.getCurrentUser().getUid())
                                    .collection("pacotes")
                                    .document(packingList.getCodigodeficha()).delete();
                        }
                     });
            firestore.collection("bipagem")
                    .document(packingList.getCodigodeficha())
                    .get().addOnSuccessListener(value -> {
                        if (value.exists()) {
                            firestore.collection("bipagem")
                                    .document(packingList.getCodigodeficha()).delete();
                        }
                    });
            }

        DocumentReference docRef = firestore.collection("rota")
                .document(mAuth.getCurrentUser().getUid())
                .collection("pacotes")
                .document(packingList.getCodigodeficha());

            Map<String, Object> packingLists = new HashMap<>();

            packingLists.put("status", "Retirado");
            packingLists.put("horaedia", packingList.getHoraedia());
            packingLists.put("codigodeficha", packingList.getCodigodeficha());
            packingLists.put("local", packingList.getLocal());
            packingLists.put("empresa", packingList.getEmpresa());
            packingLists.put("funcionario", packingList.getFuncionario());
            packingLists.put("entregador", packingList.getEntregador());
            packingLists.put("motorista", mAuth.getCurrentUser().getUid());
            packingLists.put("telefone", packingList.getTelefone());
            packingLists.put("quantidade", packingList.getQuantidade());
            packingLists.put("downloadlink", packingList.getDownloadlink());
            packingLists.put("codigosinseridos", packingList.getCodigosinseridos());

        return docRef.set(packingLists);
    }


}
