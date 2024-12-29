package com.example.rta_app.SOLID.Interfaces;

import android.graphics.Bitmap;

import com.example.rta_app.SOLID.entities.PackingList;
import com.google.android.gms.tasks.Task;
import java.util.List;

public interface IPackingListRepository {

    Task<Void> finishPackingList();

    Task<Void> updateStatusPackingList(PackingList packingList, String ocorrencia, String status);

    Task<PackingList> getPackingListToDirect(String uid);

    Task<PackingList> getPackingListToRota(String uid);

    Task<PackingList> getPackingListToBase(String uid);

    Task<List<PackingList>> getListPackingListToDirect();

    Task<List<PackingList>> getListPackingListBase();

    Task<Void> movePackingListForDelivery(PackingList packingList);

    Task<Void> updateImgLinkForFinish(Bitmap bitmap, String uid);

}
