package com.example.rta_app.SOLID.Views.Coletalista;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rta_app.R;

public class ViewRTA extends RecyclerView.ViewHolder {

    protected TextView coletanameRTAText;

    protected TextView coletacodigosRTAtext;

    protected TextView coletaQtdRTAtext;
    protected ImageView image;

    public ViewRTA(@NonNull View itemView) {
        super(itemView);
        coletanameRTAText = itemView.findViewById(R.id.coletanameRTAText);
        coletacodigosRTAtext = itemView.findViewById(R.id.coletacodigosRTAtext);

        coletaQtdRTAtext = itemView.findViewById(R.id.coletaQtdRTAtext);
        image = itemView.findViewById(R.id.image);
        image.setImageResource(R.drawable.a);
    }
}
