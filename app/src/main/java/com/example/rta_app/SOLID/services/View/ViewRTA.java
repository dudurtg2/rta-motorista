package com.example.rta_app.SOLID.services.View;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rta_app.R;

public class ViewRTA extends RecyclerView.ViewHolder {
    protected TextView nameRTAText;
    protected TextView stsRTAtext;
    protected TextView dateRTAtext;
    protected TextView cityRTAtext;
    protected TextView Empresa;

    public ViewRTA(@NonNull View itemView) {
        super(itemView);
        nameRTAText = itemView.findViewById(R.id.nameRTAText);
        stsRTAtext = itemView.findViewById(R.id.stsRTAtext);
        dateRTAtext = itemView.findViewById(R.id.dateRTAtext);
        cityRTAtext = itemView.findViewById(R.id.cityRTAtext);
        Empresa = itemView.findViewById(R.id.EmpresaRTAtext);
    }
}
