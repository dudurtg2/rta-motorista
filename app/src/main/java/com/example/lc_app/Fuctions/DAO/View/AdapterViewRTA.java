package com.example.lc_app.Fuctions.DAO.View;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.lc_app.Activitys.User.Controler.RTADetailsActivity;
import com.example.lc_app.Fuctions.DTO.ListRTADTO;
import com.example.lc_app.R;
import java.util.List;

public class AdapterViewRTA extends RecyclerView.Adapter<ViewRTA> {
  private List<ListRTADTO> list;
  private Context context;
  private int pipoca;

  public AdapterViewRTA(int item, Context context, List<ListRTADTO> listRTADTO) {
    this.context = context;
    this.list = listRTADTO;
    this.pipoca = item;
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
    if (pipoca == 1) {
      holder.itemView.setOnClickListener(v -> {
        if (item.getStatus().equals("Finalizado")) {
          Toast.makeText(context, "RTA finalizada", Toast.LENGTH_SHORT).show();
        } else {
          Intent intent = new Intent(context, RTADetailsActivity.class);
          intent.putExtra("uid", item.getName());
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(intent);
        }
      });
    }
  }

  @Override
  public int getItemCount() {
    return list.size();
  }
}
