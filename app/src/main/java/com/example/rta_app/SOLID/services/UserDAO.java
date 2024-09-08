package com.example.rta_app.SOLID.services;

import android.content.Context;
import android.widget.Toast;

import com.example.rta_app.SOLID.services.Controler.GoogleSheetsService;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserDAO {
  private final FirebaseFirestore db;
  private Context context;
  public UserDAO(Context context) {
    this.context = context;
    this.db = FirebaseFirestore.getInstance();
  }

  public Task<Void> addUser(String name, String uid) {
    Map<String, Object> user = new HashMap<>();
    user.put("nome", name);
    user.put("uid", uid);

    return db.collection("usuarios").document(uid).update(user);
  }
  public Task<Void> addFinal(String rota, String uid) {
    Map<String, Object> user = new HashMap<>();
    user.put("nome", rota);
    user.put("uid", uid);

    return db.collection("finalizados").document(uid).update(user);
  }



  public Task<Void> updateWorkHours(String nome,String uid, String cachehour, String workHours, String hour) {
    DocumentReference docRef = db.collection("usuarios")
            .document(uid)
            .collection("work_hours")
            .document(cachehour);

    return docRef.get().continueWithTask(task -> {
      if (task.isSuccessful()) {
        DocumentSnapshot document = task.getResult();

        if (document != null && document.contains(hour)) {

          Toast.makeText(context, "Você já registrou este ponto hoje", Toast.LENGTH_SHORT).show();
          return null;
        } else {
          Map<String, Object> workHoursMap = new HashMap<>();
          workHoursMap.put(hour, workHours);

          if (hour.equals("Entrada")) {
            Map<String, Object> date = new HashMap<>();
            date.put("dia_mes_ano", new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
            workHoursMap.putAll(date);
          }
          if (hour.equals("Fim")) {
            new GoogleSheetsService(context).getGoogleSheet(nome);
          }
          return docRef.set(workHoursMap, SetOptions.merge());
        }
      } else {
        throw task.getException();
      }
    });
  }
}
