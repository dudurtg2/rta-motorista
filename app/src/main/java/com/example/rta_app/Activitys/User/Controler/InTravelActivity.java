package com.example.rta_app.Activitys.User.Controler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rta_app.Fuctions.DAO.Querys.QueryRTA;
import com.example.rta_app.Fuctions.DAO.View.AdapterViewRTA;
import com.example.rta_app.R;
import com.example.rta_app.databinding.ActivityInTravelBinding;
import com.example.rta_app.databinding.ActivityMainBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class InTravelActivity extends AppCompatActivity {
  public ActivityInTravelBinding binding;
  private DocumentReference docRef, docRefRTA;
  private FirebaseFirestore firestore;
  private FirebaseAuth mAuth;
  private boolean ocorrerncias;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_in_travel);
    mAuth = FirebaseAuth.getInstance();
    firestore = FirebaseFirestore.getInstance();
    ocorrerncias = false;
    binding = ActivityInTravelBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    binding.buttonList.setOnClickListener(v -> {
      IntentIntegrator integrator = new IntentIntegrator(InTravelActivity.this);
      integrator.setCaptureActivity(CaptureActivity.class);
      integrator.setOrientationLocked(false);
      integrator.initiateScan();
    });
    binding.atualizar.setOnClickListener(v -> queryItems());
    binding.buttonFinaliza.setOnClickListener(v -> {removeFromTraver(); });
    queryItems();

    getUser();
  }
    private void removeFromTraver() {
      ocorrerncias = false;
      firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").get().addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
          for (QueryDocumentSnapshot document : task.getResult()) {
            String codigoDeFicha = document.getString("Codigo_de_ficha");
            String status = document.getString("Status");
            if (status.equals("Finalizado")) {
              removeToTraver(codigoDeFicha);
            } else {
              ocorrerncias = true;
            }
          }
          if (ocorrerncias) {
            Toast.makeText(this, "Rota contem pendencias", Toast.LENGTH_SHORT).show();
          } else {
            Toast.makeText(this, "Rota finalizada", Toast.LENGTH_SHORT).show();
          }
        }
        queryItems();
      });
    }
    private void removeToTraver(String uid) {
      docRefRTA = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);
      docRefRTA.get()
              .addOnSuccessListener(documentSnapshotRTA -> {
                if (documentSnapshotRTA.exists()) {
                  docRefRTA.delete()
                          .addOnSuccessListener(aVoid -> {
                            Map<String, Object> finalizadoData = new HashMap<>();
                            finalizadoData.put(uid, new Timestamp(new Date()));
                            firestore.collection("finalizados").document(mAuth.getCurrentUser().getUid()).update(finalizadoData);
                          })
                          .addOnFailureListener(e -> Toast.makeText(this, "Erro ao remover RTA: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
              })
              .addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
    }
  private void getUser() {
    firestore.collection("usuarios")
        .document(mAuth.getCurrentUser().getUid())
        .get()
        .addOnSuccessListener(documentSnapshot -> {
          if (documentSnapshot.exists()) {
            binding.UserNameDisplay.setText("\uD83D\uDE9B " + documentSnapshot.getString("nome"));
          } else {
            binding.UserNameDisplay.setText(mAuth.getCurrentUser().getDisplayName());
          }
        })
        .addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
  }
  public void queryItems() {
    QueryRTA queryRTATravel = new QueryRTA(this);
    queryRTATravel.readDataInTravel(dishesDTO -> {
      binding.listRTATravelview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
      binding.listRTATravelview.setAdapter(new AdapterViewRTA(1, getApplicationContext(), dishesDTO));
    });
  }
  private void confirmDocExist(String uid) {
    if (mAuth.getCurrentUser() != null) {
      docRef = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);
      docRef.get()
          .addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
              String status = documentSnapshot.getString("Status");
              if (status.equals("Finalizado")) {
                Toast.makeText(this, "RTA finalizada", Toast.LENGTH_SHORT).show();
              } else {
                Intent intent = new Intent(this, RTADetailsActivity.class);
                intent.putExtra("uid", uid);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(intent);
                finish();
              }
            } else {
              Toast.makeText(this, "RTA não encontrado", Toast.LENGTH_SHORT).show();
            }
          })
          .addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
    } else {
      Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
    if (result != null) {
      if (result.getContents() == null) {
        Toast.makeText(this, "Cancelado", Toast.LENGTH_LONG).show();
      } else {
        confirmDocExist(result.getContents());
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }
}