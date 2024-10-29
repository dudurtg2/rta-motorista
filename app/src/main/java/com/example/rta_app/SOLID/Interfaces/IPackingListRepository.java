package com.example.rta_app.SOLID.Interfaces;

import com.example.rta_app.SOLID.entities.PackingList;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IPackingListRepository {

    Task<Void> finishPackingList();

    Task<Void> updateStatusPackingList(PackingList packingList, String ocorrencia, String status);

    Task<PackingList> getPackingListToDirect(String uid);

    Task<PackingList> getPackingListToRota(String uid);

    Task<PackingList> getPackingListToBase(String uid);

    Task<List<PackingList>> getListPackingListToDirect();

    Task<List<PackingList>> getListPackingListBase();

    Task<Void> movePackingListForDelivery(PackingList packingList);

}
