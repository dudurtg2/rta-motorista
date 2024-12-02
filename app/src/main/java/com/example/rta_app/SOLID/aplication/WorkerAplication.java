package com.example.rta_app.SOLID.aplication;

import static com.example.rta_app.SOLID.services.NetworkService.isNetworkConnected;

import android.content.Context;

import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.repository.WorkerHourRepository;
import com.example.rta_app.SOLID.services.GoogleSheetsService;

public class WorkerAplication {

    private Context context;
    public WorkerAplication(Context context){
        this.context = context;

    }
    public void Finish(String nome, WorkerHous workerHous){
        if(!isNetworkConnected(context)){
            return;
        }
        new WorkerHourRepository().saveWorkerHous(new WorkerHous("", "", "", "", "", "")).addOnSuccessListener(v -> {
            new GoogleSheetsService(context).getGoogleSheet(nome, workerHous);

        });
    }


}
