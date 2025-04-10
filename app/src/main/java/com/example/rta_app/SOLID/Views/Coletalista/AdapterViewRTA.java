package com.example.rta_app.SOLID.Views.Coletalista;

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
import com.example.rta_app.SOLID.api.PackingListRepository;
import com.example.rta_app.SOLID.entities.Coletas;
import com.example.rta_app.SOLID.entities.PackingList;

import java.util.List;

public class AdapterViewRTA extends RecyclerView.Adapter<ViewRTA> {

    private final List<Coletas> coletas;
    private final Context context;

    private PackingListRepository packingListRepository;

    public AdapterViewRTA(Context context, List<Coletas> coletas) {
        this.packingListRepository = new PackingListRepository(context);
        this.context = context;
        this.coletas = coletas;

    }

    @NonNull
    @Override
    public ViewRTA onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.colete_list, parent, false);
        return new ViewRTA(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewRTA holder, int position) {


        Coletas item = coletas.get(position);
        if (item != null) {

            holder.coletanameRTAText.setText(item.getEntregador() != null ? item.getEntregador() : "Código indisponível");
            holder.coletacodigosRTAtext.setText(item.getCodigos() != null ? item.getCodigos() : "Status indisponível");
            holder.coletaQtdRTAtext.setText(item.getQtd() != null ? item.getQtd() : "0");


        }


    }


    @Override
    public int getItemCount() {
        return coletas.size();
    }

}
