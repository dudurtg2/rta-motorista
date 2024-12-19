package com.example.rta_app.SOLID.api;


import com.example.rta_app.SOLID.Interfaces.IWorkerHourRepository;
import com.example.rta_app.SOLID.entities.WorkerHous;
import com.google.android.gms.tasks.Task;

import java.io.IOException;

public class WorkerHourRepository implements IWorkerHourRepository {


    @Override
    public Task<WorkerHous> getWorkerHous() {
        return null;
    }

    @Override
    public Task<Void> saveWorkerHous(WorkerHous workerHous) {
        return null;
    }

    @Override
    public void writeToFile(String data) throws IOException {

    }

    @Override
    public String readFromFile() throws IOException {
        return "";
    }
}
