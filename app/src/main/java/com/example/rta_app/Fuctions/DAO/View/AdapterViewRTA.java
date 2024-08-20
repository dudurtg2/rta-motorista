package com.example.rta_app.Fuctions.DAO.View;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rta_app.Activitys.User.Controler.RTADetailsActivity;
import com.example.rta_app.Fuctions.DTO.ListRTADTO;
import com.example.rta_app.R;
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
    holder.dateRTAtext.setText(item.getDate());
    holder.cityRTAtext.setText(item.getCity());
    holder.Empresa.setText(item.getEnteprise());

    int color;
    switch (item.getStatus()) {
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
            Toast.makeText(context, "RTA finalizada", Toast.LENGTH_SHORT).show();
            break;
          case "Ocorrencia":
            Toast.makeText(context, "RTA em Ocorrência", Toast.LENGTH_SHORT).show();
            break;
          case "Indisponível":
            Toast.makeText(context, "RTA indisponível", Toast.LENGTH_SHORT).show();
            break;
          default:
            Intent intent = new Intent(context, RTADetailsActivity.class);
            intent.putExtra("uid", item.getName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            break;
        }
      });
    } else {
      holder.itemView.setOnClickListener(null);
    }
  }
  private void showToast(Context context, String message) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
  }
  @Override
  public int getItemCount() {
    return list.size();
  }
}
