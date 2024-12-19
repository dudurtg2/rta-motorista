package com.example.rta_app.SOLID.api;

import com.example.rta_app.SOLID.Interfaces.IPackingListRepository;
import com.example.rta_app.SOLID.entities.PackingList;
import com.google.android.gms.tasks.Task;
import java.util.List;

public class PackingListRepository implements IPackingListRepository {


    public PackingListRepository() {

    }


    @Override
    public Task<Void> finishPackingList() {
        return null;
    }

    @Override
    public Task<Void> updateStatusPackingList(PackingList packingList, String ocorrencia, String status) {
        return null;
    }

    @Override
    public Task<PackingList> getPackingListToDirect(String uid) {
        return null;
    }

    @Override
    public Task<PackingList> getPackingListToRota(String uid) {
        return null;
    }

    @Override
    public Task<PackingList> getPackingListToBase(String uid) {
        return null;
    }

    @Override
    public Task<List<PackingList>> getListPackingListToDirect() {
        return null;
    }

    @Override
    public Task<List<PackingList>> getListPackingListBase() {
        return null;
    }

    @Override
    public Task<Void> movePackingListForDelivery(PackingList packingList) {
        return null;
    }
}
