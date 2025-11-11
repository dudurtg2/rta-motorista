package com.example.rta_app.SOLID.Views.Packetlista;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rta_app.R;

public class ViewRTA extends RecyclerView.ViewHolder {

    protected TextView namePackectText;
    protected TextView entregadorNome;
    protected TextView motoristaNome;
    protected TextView dataPacket;

    protected ImageView imagePacket;

    public ViewRTA(@NonNull View itemView) {
        super(itemView);
        namePackectText = itemView.findViewById(R.id.namePackectText);
        entregadorNome = itemView.findViewById(R.id.entregadorNome);
        motoristaNome = itemView.findViewById(R.id.motoristaNome);
        dataPacket = itemView.findViewById(R.id.dataPacket);
        imagePacket = itemView.findViewById(R.id.imagePacket);

    }
}
