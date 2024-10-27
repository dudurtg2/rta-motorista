package com.example.rta_app.SOLID.repository;

import android.widget.Toast;

import com.example.rta_app.SOLID.Interfaces.IWorkerHourRepository;
import com.example.rta_app.SOLID.activitys.WorkHourActivity;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.services.GoogleSheetsService;
import com.google.android.gms.tasks.Task;
import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;


public class WorkerHourRepository implements IWorkerHourRepository {
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;


    public WorkerHourRepository() {
        this.mAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public Task<WorkerHous> getWorkerHous() {
        Task<DocumentSnapshot> task = firestore.collection("usuarios")
                .document(mAuth.getCurrentUser().getUid())
                .collection("work_hours")
                .document("cachehoras")
                .get();

        return task.continueWith(taskSnapshot -> {
            if (!taskSnapshot.isSuccessful()) {
                throw taskSnapshot.getException();
            }

            DocumentSnapshot document = taskSnapshot.getResult();

            if (document == null || !document.exists()) {
                return new WorkerHous("", "", "", "", "");
            }

            String data = document.getString("date") == null ? "" : document.getString("date");
            String entrada = document.getString("hour_first") == null ? "" : document.getString("hour_first");
            String almoco = document.getString("hour_dinner") == null ? "" : document.getString("hour_dinner");
            String saida = document.getString("hour_finish") == null ? "" : document.getString("hour_finish");
            String fim = document.getString("hour_stop") == null ? "" : document.getString("hour_stop");

            return new WorkerHous(data, entrada, almoco, saida, fim);
        });
    }


    public Task<Void> saveWorkerHous(WorkerHous workerHous) {
        DocumentReference docRef = firestore.collection("usuarios")
                .document(mAuth.getCurrentUser().getUid())
                .collection("work_hours")
                .document("cachehoras");

        return docRef.set(workerHous);
    }


}
