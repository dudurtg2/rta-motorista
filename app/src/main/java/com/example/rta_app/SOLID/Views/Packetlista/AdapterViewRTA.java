package com.example.rta_app.SOLID.Views.Packetlista;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rta_app.R;
import com.example.rta_app.SOLID.entities.Packet;
import java.util.List;
public class AdapterViewRTA extends RecyclerView.Adapter<ViewRTA> {
    private final List<Packet> packingList;
    private final Context context;

    public AdapterViewRTA(Context context, List<Packet> packingList) {
        this.context = context;
        this.packingList = packingList;
    }

    @NonNull
    @Override
    public ViewRTA onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.packet_list, parent, false);
        return new ViewRTA(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewRTA holder, int position) {


        Packet item = packingList.get(position);
        if (item != null) {

            holder.namePackectText.setText(item.getCodigo() != null ? item.getCodigo() : "Código indisponível");
            holder.entregadorNome.setText(item.getEntregador() != null ? item.getEntregador() : "Entregador indisponível");
            holder.dataPacket.setText(item.getData() != null ? item.getData().replace("T", " ").split(" ")[0] : "Data indisponível");
            holder.motoristaNome.setText(item.getRta() != null ? item.getRta() : "Motorista indisponível");

        } else {

            holder.itemView.setVisibility(View.GONE);
        }
    }


    @Override
    public int getItemCount() {
        return packingList.size();
    }

}
