package com.example.lc_app.Activitys.User.Controler;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lc_app.R;
import com.example.lc_app.databinding.ActivityRtadetailsBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RTADetailsActivity extends AppCompatActivity {
  private ActivityRtadetailsBinding binding;
  private DocumentReference docRef, docRefRTA;
  private FirebaseFirestore firestore;
  private FirebaseAuth mAuth;
  private String uid;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_rtadetails);
    binding = ActivityRtadetailsBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    firestore = FirebaseFirestore.getInstance();
    mAuth = FirebaseAuth.getInstance();
    Intent intent = getIntent();
    if (intent != null) {
      uid = intent.getStringExtra("uid");
    }
    binding.buttonRecusar.setOnClickListener(v -> statusUpdate(uid, "Recusado"));
    binding.buttonFinalizar.setOnClickListener(v -> statusUpdate(uid, "Finalizado"));
    binding.buttonOcorrencia.setOnClickListener(v -> statusUpdate(uid, "Ocorrencia"));
    getRTA(uid);
  }
  private void statusUpdate(String uid, String status) {
    docRef = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);

    docRef.get()
        .addOnSuccessListener(documentSnapshot -> {
          if (documentSnapshot.exists()) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("Status", status);

            if (status.equals("Ocorrencia")) {
              if (!binding.multiAutoCompleteTextView.getText().toString().isEmpty()) {
                updateData.put("Ocorrencia", binding.multiAutoCompleteTextView.getText().toString());
              } else {
                Toast.makeText(this, "Preencha o campo de ocorrencia", Toast.LENGTH_SHORT).show();
                return;
              }
            }
            docRef.update(updateData)
                .addOnSuccessListener(aVoid -> {
                  Toast.makeText(this, "Status atualizado para " + status, Toast.LENGTH_SHORT).show();
                  startActivity(new Intent(this, InTravelActivity.class));
                  finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao atualizar status", Toast.LENGTH_SHORT).show());
          } else {
            Toast.makeText(this, "Documento não encontrado", Toast.LENGTH_SHORT).show();
          }
        })
        .addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
  }

  private void getRTA(String uid) {
    docRef = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);
    docRef.get()
        .addOnSuccessListener(documentSnapshot -> {
          if (documentSnapshot.exists()) {
            binding.textRTA.setText(uid);
            binding.TextCidade.setText("Cidade: " + documentSnapshot.getString("Local"));
            binding.textDate.setText("Data: " + documentSnapshot.getString("Hora_e_Dia"));
            binding.textCount.setText("Quantidade: " + documentSnapshot.getString("Quantidade"));
            binding.textEntregador.setText("Entregador: " + documentSnapshot.getString("Entregador"));

            List<String> codes = (List<String>) documentSnapshot.get("Codigos inseridos");
            binding.textAllCodes.setText("Codigos inseridos:\n");
            if (codes != null) {
              for (String code : codes) {
                binding.textAllCodes.append(code + "\n");
              }
            }
          } else {
            binding.textRTA.setText("Document does not exist");
            binding.TextCidade.setText("");
            binding.textDate.setText("");
            binding.textCount.setText("");
            binding.textEntregador.setText("");
            binding.textAllCodes.setText("");
          }
        })
        .addOnFailureListener(e -> { Toast.makeText(getApplicationContext(), "Error fetching document: " + e.getMessage(), Toast.LENGTH_SHORT).show(); });
  }

  private void removeToTraver(String uid) {
    docRefRTA = firestore.collection("rota").document(mAuth.getCurrentUser().getUid()).collection("pacotes").document(uid);
    docRefRTA.get()
        .addOnSuccessListener(documentSnapshotRTA -> {
          if (documentSnapshotRTA.exists()) {
            docRefRTA.delete()
                .addOnSuccessListener(aVoid -> {
                  Toast.makeText(this, "RTA " + uid + " removida.", Toast.LENGTH_SHORT).show();

                  Map<String, Object> finalizadoData = new HashMap<>();
                  finalizadoData.put(uid, new Timestamp(new Date()));

                  firestore.collection("finalizados").document(mAuth.getCurrentUser().getUid()).update(finalizadoData);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao remover RTA: " + e.getMessage(), Toast.LENGTH_SHORT).show());
          }
        })
        .addOnFailureListener(e -> Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show());
  }
}