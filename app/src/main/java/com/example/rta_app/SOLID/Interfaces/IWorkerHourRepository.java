package com.example.rta_app.SOLID.Interfaces;

import com.example.rta_app.SOLID.entities.WorkerHous;
import com.google.android.gms.tasks.Task;

public interface IWorkerHourRepository {

    Task<WorkerHous> getWorkerHous();

    Task<Void> saveWorkerHous(WorkerHous workerHous);
}
