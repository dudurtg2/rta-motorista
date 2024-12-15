package com.example.rta_app.SOLID.aplication;

import static com.example.rta_app.SOLID.services.NetworkService.isNetworkConnected;

import android.app.AlertDialog;
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
        }).addOnFailureListener(v-> {
            WorkerHous workerHours = workerHourRepository.getWorkerHous().getResult();
            new AlertDialog.Builder(context)
                    .setTitle("Erro ao registrar ponto")
                    .setMessage("Grave manualmente seu ponto,"
                            + "\n - Entrada: " + workerHours.getHour_first()
                            + "\n - Almoço: " + workerHours.getHour_dinner()
                            + "\n - Volta: " + workerHours.getHour_stop()
                            + "\n - Fim: " + workerHours.getHour_finish()
                            + "\n - Data: " + workerHours.getDate())
                    .setNeutralButton("Ok, Entendi", (dialog, which) -> dialog.dismiss()).show();
        });
    }

    private Task<Void> UpdateWorkHours(String registroDePonto) {
        WorkerHous workerHours = workerHourRepository.getWorkerHous().getResult();
        return new GoogleSheetsService(context).getGoogleSheetTask(registroDePonto, workerHours);
    }
}
