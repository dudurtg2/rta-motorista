package com.example.rta_app.SOLID.repository;

import android.content.Context;
import android.widget.Toast;

import com.example.rta_app.SOLID.entities.ListRTADTO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class QueryRTA {
  private Context context;
  private FirebaseFirestore db;
  private List<ListRTADTO> list;
  private ArrayList<String> listRTA;
  private FirebaseAuth mAuth;

  public QueryRTA(Context context) {
    this.context = context;
    this.db = FirebaseFirestore.getInstance();
    this.list = new ArrayList<>();
    mAuth = FirebaseAuth.getInstance();
    this.listRTA = new ArrayList<>();
  }

  public interface FirestoreCallback { void onCallback(List<ListRTADTO> listRTADTO); }

  public void readData(final FirestoreCallback firestoreCallback) {
    db.collection("direcionado").document(mAuth.getCurrentUser().getUid()).collection("pacotes").get().addOnCompleteListener(task -> {
      if (task.isSuccessful()) {
        list.clear();
        for (QueryDocumentSnapshot document : task.getResult()) {
          String codigoDeFicha = document.getString("Codigo_de_ficha");
          String status = document.getString("Status");
          String data = document.getString("Hora_e_Dia");
          String city = document.getString("Local");
          String enterprise = document.getString("Empresa");
          ListRTADTO listRTADTO = new ListRTADTO(codigoDeFicha, status, data, city, enterprise);
          list.add(listRTADTO);
        }
        if (list.isEmpty()) {
          ListRTADTO listRTADTO = new ListRTADTO("Não a carga no momento", "Espere por uma nova tarefa", "", "Cunsulte a base para mais informações", "");
          list.add(listRTADTO);
        }
        firestoreCallback.onCallback(list);
      } else {
        Toast.makeText(context, "Erro ao obter documentos: " + task.getException(), Toast.LENGTH_SHORT).show();
      }
    });
  }
  public void readDataInTravel(final FirestoreCallback firestoreCallback, String filter) {
    db.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").get().addOnCompleteListener(task -> {
      if (task.isSuccessful()) {

        list.clear();

        for (QueryDocumentSnapshot document : task.getResult()) {
          String codigoDeFicha = document.getString("Codigo_de_ficha");
          String status = document.getString("Status");
          String data = document.getString("Hora_e_Dia");
          String city = document.getString("Local");
          String enterprise = document.getString("Empresa");
          ListRTADTO listRTADTO = new ListRTADTO(codigoDeFicha, status, data, city, enterprise);
          if (filter.equals("Todas as cidades")) {

            list.add(listRTADTO);
          } else if (city.equals(filter)) {
            list.add(listRTADTO);

          }
        }
        if (list.isEmpty()) {
          ListRTADTO listRTADTO = new ListRTADTO("Não a carga no momento", "Espere por uma nova tarefa", "", "Cunsulte a base para mais informações", "");
          list.add(listRTADTO);
        }

        firestoreCallback.onCallback(list);
      } else {
        Toast.makeText(context, "Erro ao obter documentos: " + task.getException(), Toast.LENGTH_SHORT).show();
      }
    });
  }




}
