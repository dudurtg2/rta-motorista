package com.example.lc_app.Fuctions.DAO.View;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lc_app.Fuctions.DTO.ListRTADTO;
import com.example.lc_app.R;

import java.util.List;

public class AdapterViewRTA extends RecyclerView.Adapter<ViewRTA> {
    private List<ListRTADTO> list;
    private Context context;

    public AdapterViewRTA(Context context, List<ListRTADTO> listRTADTO) {
        this.context = context;
        this.list = listRTADTO;
    }

    @NonNull
    @Override
    public ViewRTA onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.rta_list, parent, false);
        return new ViewRTA(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewRTA holder, int position) {
        ListRTADTO item = list.get(position);

        holder.nameRTAText.setText(item.getName());
        holder.stsRTAtext.setText(item.getStatus());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
