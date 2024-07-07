package com.example.rta_app.Fuctions.DAO.View;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rta_app.R;

public class ViewRTA extends RecyclerView.ViewHolder {
    protected TextView nameRTAText;
    protected TextView stsRTAtext;

    public ViewRTA(@NonNull View itemView) {
        super(itemView);
        nameRTAText = itemView.findViewById(R.id.nameRTAText);
        stsRTAtext = itemView.findViewById(R.id.stsRTAtext);
    }
}
