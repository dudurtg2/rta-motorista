package com.example.rta_app.SOLID.Views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rta_app.R;
import com.example.rta_app.SOLID.activitys.RTADetailsActivity;
import com.example.rta_app.SOLID.entities.PackingList;
import com.example.rta_app.SOLID.api.PackingListRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdapterViewRTA extends RecyclerView.Adapter<ViewRTA> {

    private final List<PackingList> packingList;
    private final Context context;
    private final int pipoca;
    private PackingListRepository packingListRepository;
    private FirebaseAuth mAuth;

    public AdapterViewRTA(int item, Context context, List<PackingList> packingList) {
        this.packingListRepository = new PackingListRepository(context);
        this.context = context;
        this.packingList = packingList;
        this.pipoca = item;
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ViewRTA onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.rta_list, parent, false);
        return new ViewRTA(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewRTA holder, int position) {


        PackingList item = packingList.get(position);
        if (item != null) {

            holder.nameRTAText.setText(item.getCodigodeficha() != null ? item.getCodigodeficha() : "Código indisponível");
            holder.stsRTAtext.setText(item.getStatus() != null ? item.getStatus() : "Status indisponível");
            holder.dateRTAtext.setText(item.getHoraedia() != null ? item.getHoraedia() : "Data indisponível");
            holder.cityRTAtext.setText(item.getLocal() != null ? item.getLocal() : "Cidade indisponível");
            holder.Empresa.setText(item.getEmpresa() != null ? item.getEmpresa() : "Empresa indisponível");

            int color;
            switch (item.getStatus() != null ? item.getStatus() : "") {
                case "Finalizado":
                    color = context.getResources().getColor(R.color.green);
                    break;
                case "Ocorrencia":
                    color = context.getResources().getColor(R.color.yellow);
                    break;
                case "Retirado":
                    color = context.getResources().getColor(R.color.blue);
                    break;
                case "Recusado":
                    color = context.getResources().getColor(R.color.red);
                    break;
                default:
                    color = Color.BLACK;
                    break;
            }
            holder.stsRTAtext.setTextColor(color);

            if (pipoca == 1) {
                holder.itemView.setOnClickListener(v -> {
                    switch (item.getStatus()) {
                        case "Finalizado":
                            Toast.makeText(context, "Carga finalizada", Toast.LENGTH_SHORT).show();
                            break;
                        case "Ocorrencia":
                            Toast.makeText(context, "Carga em ocorrência", Toast.LENGTH_SHORT).show();
                            break;
                        case "Espere por uma nova tarefa":
                            Toast.makeText(context, "Carga indisponível", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            Intent intent = new Intent(context, RTADetailsActivity.class);
                            intent.putExtra("uid", item.getCodigodeficha());
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                            break;
                    }
                });
            } else {
                holder.itemView.setOnClickListener(v -> {
                    if (!holder.stsRTAtext.getText().toString().equals("Status indisponível")) {
                        if (item.getStatus().equals("alocado")) {
                            Context itemViewContext = holder.itemView.getContext();
                            new AlertDialog.Builder(itemViewContext)
                                    .setTitle("Confirmação")
                                    .setMessage("Você deseja realmente adicionar essa carga a rota?")
                                    .setPositiveButton("Sim", (dialog, which) -> addToTraver(item.getCodigodeficha(), holder.getAdapterPosition()))
                                    .setNegativeButton("Não", null)
                                    .show();
                        } else {
                            Toast.makeText(context, "Carga indisponível", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        new AlertDialog.Builder(context)
                                .setTitle("Alerta")
                                .setMessage("Não há carga disponível no momento.\nDirija-se à base para mais detalhes.")
                                .setNegativeButton("OK", null)
                                .show();

                    }
                });

            }
        } else {

            holder.itemView.setVisibility(View.GONE);
        }
    }

    public void addToTraver(String uid, int position) {
        packingListRepository.getPackingListToDirect(uid).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot != null) {
                String codigoDeFicha = documentSnapshot.getCodigodeficha();
                String motorista = documentSnapshot.getMotorista();
                String status = documentSnapshot.getStatus();

                if (codigoDeFicha != null && !codigoDeFicha.isEmpty()) {


                        packingListRepository.movePackingListForDelivery(documentSnapshot)
                                .addOnSuccessListener(vo -> notifyItemRemoved(position));

                } else {
                    Toast.makeText(context, "Carga indisponível", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Carga indisponível", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(va -> Toast.makeText(context, "Carga indisponível", Toast.LENGTH_SHORT).show());
    }


    @Override
    public int getItemCount() {
        return packingList.size();
    }

}
