package com.example.rta_app.Activitys.User.Controler;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rta_app.R;
import com.example.rta_app.databinding.ActivityRtadetailsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

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
    binding.textRTA.setOnClickListener(v -> downloadRTA(uid) );
    getRTA(uid);
  }
  private void downloadRTA(String uid) {
    docRefRTA = firestore.collection("rota")
            .document(mAuth.getCurrentUser().getUid())
            .collection("pacotes")
            .document(uid);

    docRefRTA.get().addOnSuccessListener(documentSnapshot -> {
      if (documentSnapshot.exists()) {
        String downloadLink = documentSnapshot.getString("Download_link");
        if (downloadLink != null) {
          Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadLink));
          startActivity(intent);
        } else {
          Toast.makeText(this, "Download link not available", Toast.LENGTH_SHORT).show();
        }
      } else {
        Toast.makeText(this, "Document does not exist", Toast.LENGTH_SHORT).show();
      }
    }).addOnFailureListener(e -> {
      Toast.makeText(this, "Failed to fetch document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    });
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
            binding.textRTA.setText(uid + " \uD83D\uDCBE");
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

}