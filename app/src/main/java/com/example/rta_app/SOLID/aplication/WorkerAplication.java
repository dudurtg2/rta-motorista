package com.example.rta_app.SOLID.aplication;

import static com.example.rta_app.SOLID.services.NetworkService.isNetworkConnected;

import android.content.Context;

import com.example.rta_app.SOLID.Interfaces.IWorkerHourRepository;
import com.example.rta_app.SOLID.entities.WorkerHous;  // Corrected class name
import com.example.rta_app.SOLID.repository.WorkerHourRepository;
import com.example.rta_app.SOLID.services.GoogleSheetsService;
import com.google.android.gms.tasks.Task;

public class WorkerAplication {

    private Context context;
    private IWorkerHourRepository workerHourRepository;

    public WorkerAplication(Context context) {
        this.context = context;
        this.workerHourRepository = new WorkerHourRepository(context);
    }

    public Task<Void> Finish(String nome) {
        if (!isNetworkConnected(context)) {
            return null;
        }
        return UpdateWorkHours(nome).onSuccessTask(task -> {
            WorkerHous workerHours = new WorkerHous("", "", "", "", "", "");
            return new WorkerHourRepository(context).saveWorkerHous(workerHours);
        });
    }

    private Task<Void> UpdateWorkHours(String registroDePonto) {
        WorkerHous workerHours = workerHourRepository.getWorkerHous().getResult();
        return new GoogleSheetsService(context).getGoogleSheetTask(registroDePonto, workerHours);
    }
}
