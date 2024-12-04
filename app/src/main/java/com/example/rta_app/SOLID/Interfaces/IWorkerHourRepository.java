package com.example.rta_app.SOLID.Interfaces;

import com.example.rta_app.SOLID.entities.WorkerHous;
import com.google.android.gms.tasks.Task;

import java.io.IOException;

public interface IWorkerHourRepository {

    Task<WorkerHous> getWorkerHous();

    Task<Void> saveWorkerHous(WorkerHous workerHous);

    void writeToFile(String data) throws IOException;
    String readFromFile() throws IOException;
}
