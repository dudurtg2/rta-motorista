package com.example.rta_app.SOLID.aplication;

import static com.example.rta_app.SOLID.services.NetworkService.isNetworkConnected;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import com.example.rta_app.SOLID.entities.WorkerHous;
import com.example.rta_app.SOLID.api.WorkerHourRepository;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

public class WorkerAplication {

    private static final String TAG = "WorkerAplication";

    private final Context context;
    private final WorkerHourRepository workerHourRepository;

    public WorkerAplication(Context context) {
        Log.d(TAG, "Constructor: inicializando WorkerAplication");
        this.context = context;
        this.workerHourRepository = new WorkerHourRepository(context);
    }

    /**
     * Finaliza o registro de ponto e "reseta" o WorkerHous no repositório.
     *
     * @param nome nome do usuário (só para log ou mensagem de erro)
     * @return Task<Void> ou null se sem internet
     */
    public Task<Void> Finish(String nome) {
        Log.d(TAG, "Finish(): chamado para usuário = " + nome);

        if (!isNetworkConnected(context)) {
            Log.w(TAG, "Finish(): sem conexão com a internet, retornando null");
            return null;
        }

        // 1) Ler WorkerHous atual
        return workerHourRepository.getWorkerHous()

                // 2) Ao obter, salvar no servidor com saveHors()
                .continueWithTask((Continuation<WorkerHous, Task<Void>>) task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    WorkerHous current = task.getResult();
                    Log.d(TAG, "Finish(): WorkerHous atual obtido = " + current);
                    return workerHourRepository.saveHors(current)
                            .addOnSuccessListener(v -> Log.d(TAG, "Finish(): saveHors() bem-sucedido"));
                })

                // 3) Depois de salvar, fazer o reset local com saveWorkerHous()
                .continueWithTask((Continuation<Void, Task<Void>>) task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    Log.d(TAG, "Finish(): criando WorkerHous vazio para reset");
                    WorkerHous reset = new WorkerHous("", "", "", "", "", "");
                    return workerHourRepository.saveWorkerHous(reset)
                            .addOnSuccessListener(v -> Log.d(TAG, "Finish(): reset WorkerHous salvo com sucesso"));
                })

                // 4) Tratar falhas em qualquer etapa
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Finish(): erro na cadeia de tasks", e);
                    // Em caso de erro, mostra diálogo com valores para gravação manual
                    // Note: getWorkerHous().getResult() aqui pode ser null se a leitura falhou,
                    // mas é só para preencher o diálogo; você pode ajustar conforme quiser.
                    WorkerHous workerHours = workerHourRepository.getWorkerHous().getResult();
                    Log.d(TAG, "Finish(): exibindo diálogo de erro com dados = " + workerHours);
                    new AlertDialog.Builder(context)
                            .setTitle("Erro ao registrar ponto")
                            .setMessage("Grave manualmente seu ponto," +
                                    "\n - Entrada: " + workerHours.getHour_first() +
                                    "\n - Almoço: " + workerHours.getHour_dinner() +
                                    "\n - Volta:  " + workerHours.getHour_finish() +
                                    "\n - Fim:    " + workerHours.getHour_stop() +
                                    "\n - Data:   " + workerHours.getDate())
                            .setNeutralButton("Ok, Entendi", (dialog, which) -> dialog.dismiss())
                            .show();
                });
    }

}
