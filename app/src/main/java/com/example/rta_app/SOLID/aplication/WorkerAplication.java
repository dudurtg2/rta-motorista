package com.example.rta_app.SOLID.aplication;

import static com.example.rta_app.SOLID.services.NetworkService.isNetworkConnected;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import com.example.rta_app.SOLID.Interfaces.IWorkerHourRepository;
import com.example.rta_app.SOLID.activitys.WorkHourActivity;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.repository.WorkerHourRepository;
import com.example.rta_app.SOLID.services.GoogleSheetsService;

public class WorkerAplication {

    private Context context;
    private IWorkerHourRepository workerHourRepository;;
    public WorkerAplication(Context context){
        workerHourRepository = new WorkerHourRepository(context);
        this.context = context;

    }
    public void Finish(String nome){
        if(!isNetworkConnected(context)){
            return;
        }
        WorkerHous workerHous = workerHourRepository.getWorkerHous().getResult();
        new GoogleSheetsService(context).getGoogleSheetTask(nome, workerHous).addOnSuccessListener(v -> {
            new WorkerHourRepository(context).saveWorkerHous(new WorkerHous("", "", "", "", "", "")).addOnSuccessListener(v2 -> {
                Toast.makeText(context, "Ponto registrado com sucesso.", Toast.LENGTH_SHORT).show();

            });
        }).addOnFailureListener(e -> {
            new AlertDialog.Builder(context)
                    .setTitle("Erro ao registrar ponto")
                    .setMessage("Grave manualmente seu ponto,"
                            + "\n - Entrada: " + workerHous.getHour_first()
                            + "\n - Almoço: " + workerHous.getHour_dinner()
                            + "\n - Volta: " + workerHous.getHour_stop()
                            + "\n - Fim: " + workerHous.getHour_finish()
                            + "\n - Data: " + workerHous.getDate())
                    .setNeutralButton("Ok, Entendi", (dialog, which) -> dialog.dismiss()).show();
        });
    }


}
